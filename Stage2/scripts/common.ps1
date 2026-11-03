$ErrorActionPreference = 'Stop'
$Stage2Root = Split-Path -Parent $PSScriptRoot

function Find-Stage2Jdk {
    $candidates = @()
    if ($env:STAGE2_JAVA_HOME) { $candidates += $env:STAGE2_JAVA_HOME }
    if ($env:JAVA_HOME) { $candidates += $env:JAVA_HOME }
    $compiler = Get-Command javac -ErrorAction SilentlyContinue
    if ($compiler) { $candidates += (Split-Path -Parent (Split-Path -Parent $compiler.Source)) }
    foreach ($base in @("$env:ProgramFiles\Eclipse Adoptium", "$env:ProgramFiles\Java", "$env:ProgramFiles\Microsoft", "$env:USERPROFILE\.jdks")) {
        if (Test-Path -LiteralPath $base) {
            $candidates += @(Get-ChildItem -LiteralPath $base -Directory -ErrorAction SilentlyContinue | Select-Object -ExpandProperty FullName)
        }
    }
    foreach ($candidate in ($candidates | Select-Object -Unique)) {
        $javac = Join-Path $candidate 'bin\javac.exe'
        if (Test-Path -LiteralPath $javac) {
            $version = & $javac -version 2>&1
            if ($LASTEXITCODE -eq 0 -and "$version" -match '^javac 21\.') { return $candidate }
        }
    }
    throw 'Java 21 JDK not found. Set STAGE2_JAVA_HOME for this PowerShell session to your existing JDK 21 directory.'
}

function Write-Stage2SourceList($Files, [string]$Path) {
    $lines = @($Files | ForEach-Object { '"' + $_.FullName.Replace('\', '/') + '"' })
    [IO.File]::WriteAllLines($Path, $lines, [Text.UTF8Encoding]::new($false))
}

function Assert-Stage2Exit([string]$Action) {
    if ($LASTEXITCODE -ne 0) { throw "$Action failed (exit $LASTEXITCODE)." }
}

function Get-Stage2Gson {
    $dependencyDir = Join-Path $Stage2Root 'build\dependencies'
    New-Item -ItemType Directory -Force -Path $dependencyDir | Out-Null
    $dependencyJar = Join-Path $dependencyDir 'gson-2.13.2.jar'
    if (-not (Test-Path -LiteralPath $dependencyJar)) {
        Invoke-WebRequest -UseBasicParsing 'https://repo.maven.apache.org/maven2/com/google/code/gson/gson/2.13.2/gson-2.13.2.jar' -OutFile $dependencyJar
    }
    if ((Get-FileHash -LiteralPath $dependencyJar -Algorithm SHA256).Hash -ne 'DD0CE1B55A3ED2080CB70F9C655850CDA86C206862310009DCB5E5C95265A5E0') {
        throw 'Gson checksum mismatch. Remove only build/dependencies/gson-2.13.2.jar and rebuild.'
    }
    return $dependencyJar
}
