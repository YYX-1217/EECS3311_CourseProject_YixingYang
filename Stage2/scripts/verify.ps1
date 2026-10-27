param([switch]$Gui, [switch]$DependencyFree)
& (Join-Path $PSScriptRoot 'build.ps1')
. (Join-Path $PSScriptRoot 'common.ps1')
$jdk = Find-Stage2Jdk
$testClasses = Join-Path $Stage2Root 'build\test-classes'
New-Item -ItemType Directory -Force -Path $testClasses | Out-Null
$junit = $null
$hamcrest = $null
if (-not $DependencyFree) {
    $pluginRoots = @(Get-ChildItem -Path "$env:USERPROFILE\.vscode\extensions\redhat.java-*\server\plugins" -Directory -ErrorAction SilentlyContinue)
    foreach ($pluginRoot in $pluginRoots) {
        $junit = Get-ChildItem -LiteralPath $pluginRoot.FullName -Filter 'org.junit_4.13.2*.jar' | Select-Object -First 1
        $hamcrest = Get-ChildItem -LiteralPath $pluginRoot.FullName -Filter 'org.hamcrest_*.jar' | Select-Object -First 1
        if ($junit -and $hamcrest) { break }
    }
}
$useJunit = $junit -and $hamcrest
$classPath = 'build\classes;build\test-classes'
if ($useJunit) { $classPath += ";$($junit.FullName);$($hamcrest.FullName)" }
$sources = @(Get-ChildItem -LiteralPath (Join-Path $Stage2Root 'src\test\java') -Filter '*.java' -Recurse |
    Where-Object { $useJunit -or $_.Name -ne 'FoundationTest.java' } | Sort-Object FullName)
Write-Stage2SourceList $sources (Join-Path $Stage2Root 'build\test-sources.txt')
Push-Location -LiteralPath $Stage2Root
try {
    & (Join-Path $jdk 'bin\javac.exe') --release 21 -encoding UTF-8 -cp $classPath -d '.\build\test-classes' '@build/test-sources.txt'
    Assert-Stage2Exit 'Test compilation'
    if ($useJunit) {
        & (Join-Path $jdk 'bin\java.exe') -cp $classPath org.junit.runner.JUnitCore ca.yorku.eecs3311.usedcar.FoundationTest
    } else {
        Write-Host 'Using dependency-free verification (JUnit not selected/available).'
        & (Join-Path $jdk 'bin\java.exe') -cp $classPath ca.yorku.eecs3311.usedcar.FoundationChecks
    }
    Assert-Stage2Exit 'Deterministic verification'
    if ($Gui) {
        & (Join-Path $jdk 'bin\java.exe') -cp $classPath ca.yorku.eecs3311.usedcar.GuiSmoke
        Assert-Stage2Exit 'GUI smoke verification'
    }
} finally { Pop-Location }
