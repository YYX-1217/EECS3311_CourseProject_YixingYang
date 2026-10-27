param([ValidateSet('demo','cli','gui','help')][string]$Mode = 'demo')
. (Join-Path $PSScriptRoot 'common.ps1')
$jdk = Find-Stage2Jdk
$jar = Join-Path $Stage2Root 'build\used-car-assistant.jar'
if (-not (Test-Path -LiteralPath $jar)) { & (Join-Path $PSScriptRoot 'build.ps1') }
& (Join-Path $jdk 'bin\java.exe') '-Dfile.encoding=UTF-8' -jar $jar "--$Mode"
Assert-Stage2Exit 'Application'
