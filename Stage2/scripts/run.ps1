param([ValidateSet('demo','cli','gui','help')][string]$Mode = 'demo')
. (Join-Path $PSScriptRoot 'common.ps1')
. (Join-Path $PSScriptRoot 'gemini-credentials.ps1')
$jdk = Find-Stage2Jdk
$jar = Join-Path $Stage2Root 'build\used-car-assistant.jar'
if (-not (Test-Path -LiteralPath $jar)) { & (Join-Path $PSScriptRoot 'build.ps1') }
$launch = { Invoke-Stage2Java -Java (Join-Path $jdk 'bin\java.exe') -Jar $jar -Mode $Mode }
if ($Mode -eq 'help') { & $launch }
else { Invoke-WithStage2Gemini -Action $launch }
