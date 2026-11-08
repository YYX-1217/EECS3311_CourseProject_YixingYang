param([switch]$SkipBuild, [switch]$Gui)
$ErrorActionPreference='Stop'
. (Join-Path $PSScriptRoot 'common.ps1')
. (Join-Path $PSScriptRoot 'gemini-credentials.ps1')
# Invoke in a separate PowerShell process. Never inspect the user's actual saved credential.
[Environment]::SetEnvironmentVariable('GEMINI_API_KEY',$null,'Process')
$env:GEMINI_MODEL='gemini-offline-launcher-test'
$env:STAGE2_TEST_EXPECTED_MODEL=$env:GEMINI_MODEL
if(-not $SkipBuild){& (Join-Path $PSScriptRoot 'build.ps1')}
$jdk=Find-Stage2Jdk;$java=Join-Path $jdk 'bin\java.exe'
$testRoot=Join-Path ([Environment]::GetFolderPath([Environment+SpecialFolder]::LocalApplicationData)) ('EECS3311\UsedCarAssistant.Tests-'+[guid]::NewGuid().ToString('N'))
$testRoot=[IO.Path]::GetFullPath($testRoot)
$work=Join-Path $Stage2Root ('build\verification\launcher-'+[guid]::NewGuid().ToString('N'))
$classes=Join-Path $work 'classes';New-Item -ItemType Directory -Path $classes -Force | Out-Null
& (Join-Path $jdk 'bin\javac.exe') --release 21 -encoding UTF-8 -cp (Join-Path $Stage2Root 'build\classes') -d $classes (Join-Path $Stage2Root 'src\test\java\ca\yorku\eecs3311\usedcar\LauncherProbe.java')
Assert-Stage2Exit 'Launcher probe compilation'
$fakeJar=Join-Path $work 'fixture.jar'
& (Join-Path $jdk 'bin\jar.exe') --create --file $fakeJar --main-class ca.yorku.eecs3311.usedcar.LauncherProbe -C $classes .
Assert-Stage2Exit 'Launcher fixture packaging'
$script:passed=0
$key='fictional-'+[guid]::NewGuid().ToString('N');$replacement='fictional-'+[guid]::NewGuid().ToString('N');$explicit='fictional-'+[guid]::NewGuid().ToString('N')
function Assert-Check([bool]$Condition,[string]$Message){if(-not $Condition){throw $Message}}
function Test-Case([string]$Name,[scriptblock]$Check){& $Check;$script:passed++;Write-Host "PASS: $Name"}
function Hash-Key([string]$Value){$hash=[Security.Cryptography.SHA256]::Create();try{return ([BitConverter]::ToString($hash.ComputeHash([Text.Encoding]::UTF8.GetBytes($Value)))).Replace('-','').ToLowerInvariant()}finally{$hash.Dispose()}}
function Enroll-Test([string]$Value){$secure=ConvertTo-SecureString $Value -AsPlainText -Force;try{Save-Stage2GeminiCredential $secure $testRoot}finally{$secure.Dispose()}}
function Probe([string]$Expected){$env:STAGE2_TEST_EXPECTED_HASH=$Expected;& $java -cp ($classes+';'+(Join-Path $Stage2Root 'build\classes')) ca.yorku.eecs3311.usedcar.LauncherProbe;Assert-Stage2Exit 'Child environment probe'}
function Assert-NoTemporaryKey {Assert-Check ([string]::IsNullOrEmpty($env:GEMINI_API_KEY)) 'Temporary key remains after return.'}
try{
 Test-Case 'Missing credential: fallback and setup guidance without persistence' {
  $output=Invoke-WithStage2Gemini -Directory $testRoot -Action {Probe 'NONE'} 6>&1 | Out-String
  Assert-Check ($output.Contains('NOT CONFIGURED') -and $output.Contains('setup-gemini.ps1') -and -not(Test-Path -LiteralPath $testRoot)) 'Missing-store behavior failed.'
 }
 Test-Case 'DPAPI enrollment outside repository; reject in-repository destination' {
  Enroll-Test $key;$cipher=[IO.File]::ReadAllText((Join-Path $testRoot 'gemini-api-key.dpapi'))
  Assert-Check (-not $cipher.Contains($key) -and $cipher.Length -gt 50) 'Credential not encrypted.'
  Assert-Check (-not $testRoot.StartsWith((Split-Path -Parent $Stage2Root),[StringComparison]::OrdinalIgnoreCase)) 'Store inside repo.'
  $rejected=$false;try{Assert-Stage2CredentialPath (Join-Path $Stage2Root 'credentials') | Out-Null}catch{$rejected=$true};Assert-Check $rejected 'Repository path accepted.'
 }
 Test-Case 'Directory and file ACLs restrict inheritance and principals' {
  foreach($path in @($testRoot,(Join-Path $testRoot 'gemini-api-key.dpapi'))){
   $acl=Get-Acl -LiteralPath $path;Assert-Check $acl.AreAccessRulesProtected 'ACL inheritance enabled.'
   $sid=[Security.Principal.WindowsIdentity]::GetCurrent().User.Value
   foreach($rule in $acl.GetAccessRules($true,$true,[Security.Principal.SecurityIdentifier])){Assert-Check (@($sid,'S-1-5-18') -contains $rule.IdentityReference.Value) 'Unexpected ACL principal.'}
  }
 }
 Test-Case 'Current user decrypts; Java inherits secret; conversion memory released before launch' {
  $output=Invoke-WithStage2Gemini -Directory $testRoot -Action {
   Assert-Check ($pointer -eq [IntPtr]::Zero -and $null -eq $secret -and $null -eq $plain) 'Conversion state retained.'
   Probe (Hash-Key $key)
  } *>&1 | Out-String
  Assert-Check ($output.Contains('PROBE_OK') -and -not $output.Contains($key)) 'Probe failed or output leaked credential.';Assert-NoTemporaryKey
 }
 Test-Case 'Explicit environment precedence and model/caller preservation' {
  $env:GEMINI_API_KEY=$explicit
  try{Invoke-WithStage2Gemini -Directory (Join-Path $Stage2Root 'must-not-read') -Action {Probe (Hash-Key $explicit)};Assert-Check ($env:GEMINI_API_KEY -ceq $explicit) 'Caller key changed.'}finally{Remove-Item Env:GEMINI_API_KEY}
 }
 Test-Case 'Replacement decrypts and ciphertext survives ordinary cleanup' {
  Enroll-Test $replacement;Invoke-WithStage2Gemini -Directory $testRoot -Action {Probe (Hash-Key $replacement)};Assert-NoTemporaryKey
  Assert-Check (Test-Path -LiteralPath (Join-Path $testRoot 'gemini-api-key.dpapi')) 'Enrollment deleted on exit.'
 }
 Test-Case 'GUI launcher waits for actual Java termination and clears temporary environment' {
  $env:STAGE2_TEST_EXPECTED_HASH=Hash-Key $replacement;$timer=[Diagnostics.Stopwatch]::StartNew()
  Invoke-WithStage2Gemini -Directory $testRoot -Action {Invoke-Stage2Java -Java $java -Jar $fakeJar -Mode gui}
  Assert-Check ($timer.ElapsedMilliseconds -ge 650) 'Launcher returned before child.';Assert-NoTemporaryKey
 }
 Test-Case 'Cleanup after process start failure' {
  $failed=$false;try{Invoke-WithStage2Gemini -Directory $testRoot -Action {Invoke-Stage2Java -Java (Join-Path $work 'missing.exe') -Jar $fakeJar -Mode gui}}catch{$failed=$true;Assert-Check (-not $_.ToString().Contains($replacement)) 'Failure leaked credential.'}
  Assert-Check $failed 'Missing executable did not fail.';Assert-NoTemporaryKey
 }
 Test-Case 'Cleanup after abnormal child exit' {
  $env:STAGE2_TEST_CHILD_MODE='FAIL';$failed=$false
  try{Invoke-WithStage2Gemini -Directory $testRoot -Action {Invoke-Stage2Java -Java $java -Jar $fakeJar -Mode gui}}catch{$failed=$true}finally{Remove-Item Env:STAGE2_TEST_CHILD_MODE}
  Assert-Check $failed 'Nonzero child exit not reported.';Assert-NoTemporaryKey
 }
 Test-Case 'Corrupted storage produces sanitized warning and fallback' {
  [IO.File]::WriteAllText((Join-Path $testRoot 'gemini-api-key.dpapi'),'corrupt-'+$key)
  $output=Invoke-WithStage2Gemini -Directory $testRoot -Action {Probe 'NONE'} *>&1 | Out-String
  Assert-Check ($output.Contains('Saved Gemini credential unavailable') -and $output.Contains('PROBE_OK') -and -not $output.Contains($key)) 'Corrupt storage handling failed.';Assert-NoTemporaryKey
 }
 Test-Case 'Unreadable storage produces safe fallback' {
  Enroll-Test $replacement;$stream=[IO.File]::Open((Join-Path $testRoot 'gemini-api-key.dpapi'),[IO.FileMode]::Open,[IO.FileAccess]::Read,[IO.FileShare]::None)
  try{$output=Invoke-WithStage2Gemini -Directory $testRoot -Action {Probe 'NONE'} *>&1 | Out-String;Assert-Check ($output.Contains('unavailable') -and $output.Contains('PROBE_OK')) 'Unreadable fallback failed.'}finally{$stream.Dispose()};Assert-NoTemporaryKey
 }
 Test-Case 'Explicit deletion prevents future automatic loading' {
  Remove-Stage2GeminiCredential $testRoot;Invoke-WithStage2Gemini -Directory $testRoot -Action {Probe 'NONE'}
  Assert-Check (-not(Test-Path -LiteralPath (Join-Path $testRoot 'gemini-api-key.dpapi'))) 'Credential not removed.'
 }
 Test-Case 'Failed enrollment preserves existing protected credential' {
  Enroll-Test $replacement;$failed=$false;$empty=New-Object Security.SecureString
  try{Save-Stage2GeminiCredential $empty $testRoot}catch{$failed=$true}finally{$empty.Dispose()}
  Assert-Check $failed 'Empty key accepted.';Invoke-WithStage2Gemini -Directory $testRoot -Action {Probe (Hash-Key $replacement)};Assert-NoTemporaryKey
 }
 Test-Case 'Actual run.ps1 demo and parent environment remain compatible' {
  $env:GEMINI_API_KEY=$explicit
  try{$output=& powershell -NoProfile -ExecutionPolicy Bypass -File (Join-Path $PSScriptRoot 'run.ps1') -Mode demo 2>&1 | Out-String;Assert-Stage2Exit 'Demo launch'
   Assert-Check ($output.Contains('CONFIGURED') -and -not $output.Contains($explicit) -and $env:GEMINI_API_KEY -ceq $explicit) 'Demo or parent environment mismatch.'
  }finally{Remove-Item Env:GEMINI_API_KEY}
 }
 Test-Case 'Existing CLI status/quit: configured, not authenticated, no network' {
  $env:GEMINI_API_KEY=$explicit
  try{$output=@('status','quit') | & $java -jar (Join-Path $Stage2Root 'build\used-car-assistant.jar') --cli | Out-String;Assert-Stage2Exit 'CLI status'
   Assert-Check ($output.Contains('CONFIGURED: not live-verified') -and -not $output.Contains($explicit)) 'CLI status mismatch.'
  }finally{Remove-Item Env:GEMINI_API_KEY}
 }
 if($Gui){
  Test-Case 'Real Swing initial status displays configured but not live-verified' {
   $env:STAGE2_TEST_EXPECTED_HASH=Hash-Key $replacement
   Invoke-WithStage2Gemini -Directory $testRoot -Action {& $java -cp ($classes+';'+(Join-Path $Stage2Root 'build\classes')) ca.yorku.eecs3311.usedcar.LauncherProbe --inspect-gui;Assert-Stage2Exit 'GUI configured status'};Assert-NoTemporaryKey
  }
  Test-Case 'Actual GUI close terminates Java and waiting launcher, with cleanup' {
   $runner=Join-Path $work 'gui-session.ps1';$marker=Join-Path $work 'gui-cleanup.txt'
   @'
param($Helpers,$Java,$Jar,$Store,$Marker)
$ErrorActionPreference='Stop'
. $Helpers
[Environment]::SetEnvironmentVariable('GEMINI_API_KEY',$null,'Process')
try{Invoke-WithStage2Gemini -Directory $Store -Action {Invoke-Stage2Java -Java $Java -Jar $Jar -Mode gui}}
finally{if([string]::IsNullOrEmpty($env:GEMINI_API_KEY)){[IO.File]::WriteAllText($Marker,'CLEAN')}else{[IO.File]::WriteAllText($Marker,'FAILED')}}
'@ | Set-Content -LiteralPath $runner -Encoding UTF8
   $argsText='-NoProfile -ExecutionPolicy Bypass -File "'+$runner+'" -Helpers "'+(Join-Path $PSScriptRoot 'gemini-credentials.ps1')+'" -Java "'+$java+'" -Jar "'+(Join-Path $Stage2Root 'build\used-car-assistant.jar')+'" -Store "'+$testRoot+'" -Marker "'+$marker+'"'
   $launcher=Start-Process powershell -ArgumentList $argsText -WindowStyle Hidden -PassThru;$child=$null
   try{
    $deadline=[DateTime]::UtcNow.AddSeconds(20)
    while([DateTime]::UtcNow -lt $deadline){
     $candidate=Get-CimInstance Win32_Process -Filter "ParentProcessId=$($launcher.Id) AND Name='java.exe'" | Select-Object -First 1
     if($candidate){$child=Get-Process -Id $candidate.ProcessId -ErrorAction SilentlyContinue;if($child){$child.Refresh();if($child.MainWindowHandle -ne 0){break}}};Start-Sleep -Milliseconds 100
    }
    Assert-Check ($null -ne $child -and $child.MainWindowHandle -ne 0 -and -not $launcher.HasExited) 'GUI not alive under waiting launcher.'
    Assert-Check ($child.CloseMainWindow()) 'Could not close test-owned GUI.'
    Assert-Check ($child.WaitForExit(10000)) 'Java stayed alive after GUI close.'
    Assert-Check ($launcher.WaitForExit(10000)) 'Launcher stayed alive after Java exit.'
    Assert-Check ($launcher.ExitCode -eq 0 -and [IO.File]::ReadAllText($marker) -ceq 'CLEAN') 'Real GUI cleanup failed.'
   }finally{
    if($child){if(-not $child.HasExited){$child.Kill()};$child.Dispose()}
    if(-not $launcher.HasExited){$launcher.Kill()};$launcher.Dispose()
   }
  }
 }
 Write-Host "PASS: $script:passed Windows credential/launcher checks; no real API request or real saved credential used."
}finally{
 foreach($name in @('GEMINI_API_KEY','STAGE2_TEST_EXPECTED_HASH','STAGE2_TEST_EXPECTED_MODEL','STAGE2_TEST_CHILD_MODE')){[Environment]::SetEnvironmentVariable($name,$null,'Process')}
 $allowed=Join-Path ([Environment]::GetFolderPath([Environment+SpecialFolder]::LocalApplicationData)) 'EECS3311'
 $resolved=[IO.Path]::GetFullPath($testRoot)
 if((Split-Path -Parent $resolved) -ceq $allowed -and (Split-Path -Leaf $resolved) -match '^UsedCarAssistant\.Tests-[a-f0-9]{32}$'){
  if(Test-Path -LiteralPath $resolved){Remove-Item -LiteralPath $resolved -Recurse -Force}
 }else{throw 'Refusing to remove an unverified test storage path.'}
 $key=$null;$replacement=$null;$explicit=$null
}
