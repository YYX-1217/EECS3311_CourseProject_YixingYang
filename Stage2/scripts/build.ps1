. (Join-Path $PSScriptRoot 'common.ps1')
$jdk = Find-Stage2Jdk
$build = Join-Path $Stage2Root 'build'
$classes = Join-Path $build 'classes'
New-Item -ItemType Directory -Force -Path $classes | Out-Null
$sources = @(Get-ChildItem -LiteralPath (Join-Path $Stage2Root 'src\main\java') -Filter '*.java' -Recurse | Sort-Object FullName)
$sourceList = Join-Path $build 'main-sources.txt'
Write-Stage2SourceList $sources $sourceList
Push-Location -LiteralPath $Stage2Root
try {
    & (Join-Path $jdk 'bin\javac.exe') '-J-Duser.language=en' '-J-Duser.country=US' --release 21 -encoding UTF-8 -d '.\build\classes' '@build/main-sources.txt'
    Assert-Stage2Exit 'Compilation'
    Copy-Item -Path 'src\main\resources\*' -Destination $classes -Recurse -Force
    & (Join-Path $jdk 'bin\jar.exe') --create --file 'build/used-car-assistant.jar' --main-class ca.yorku.eecs3311.usedcar.app.Main -C 'build/classes' .
    Assert-Stage2Exit 'JAR packaging'
} finally { Pop-Location }
Write-Host "Built with JDK 21: $jdk"
Write-Host "Application: $build\used-car-assistant.jar"
