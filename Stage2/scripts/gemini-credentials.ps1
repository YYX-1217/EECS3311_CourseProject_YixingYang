# Windows-only persistent enrollment; Java still receives process environment variables only.
function Get-Stage2CredentialDirectory {
    if ([Environment]::OSVersion.Platform -ne [PlatformID]::Win32NT) { throw 'Saved Gemini credentials require Windows.' }
    $local = [Environment]::GetFolderPath([Environment+SpecialFolder]::LocalApplicationData)
    if ([string]::IsNullOrWhiteSpace($local)) { throw 'Windows local application storage is unavailable.' }
    return Join-Path $local 'EECS3311\UsedCarAssistant'
}

function Assert-Stage2CredentialPath([string]$Directory) {
    if ([Environment]::OSVersion.Platform -ne [PlatformID]::Win32NT) { throw 'Saved Gemini credentials require Windows.' }
    $full = [IO.Path]::GetFullPath($Directory).TrimEnd('\')
    $local = [IO.Path]::GetFullPath([Environment]::GetFolderPath([Environment+SpecialFolder]::LocalApplicationData)).TrimEnd('\')
    $repo = [IO.Path]::GetFullPath((Split-Path -Parent (Split-Path -Parent $PSScriptRoot))).TrimEnd('\')
    if (-not $full.StartsWith($local + '\', [StringComparison]::OrdinalIgnoreCase) -or
        $full.Equals($repo, [StringComparison]::OrdinalIgnoreCase) -or $full.StartsWith($repo + '\', [StringComparison]::OrdinalIgnoreCase)) {
        throw 'Credential storage must be outside the repository and inside Windows local application storage.'
    }
    # Refuse redirected folders/files before writing or decrypting any credential.
    $cursor = $full
    while ($cursor) {
        if (Test-Path -LiteralPath $cursor) {
            if ((Get-Item -LiteralPath $cursor -Force -ErrorAction Stop).Attributes -band [IO.FileAttributes]::ReparsePoint) {
                throw 'Redirected credential storage is not supported.'
            }
        }
        $cursor = Split-Path -Parent $cursor
    }
    $file = Join-Path $full 'gemini-api-key.dpapi'
    if (Test-Path -LiteralPath $file) {
        $item = Get-Item -LiteralPath $file -Force -ErrorAction Stop
        if ($item.PSIsContainer -or ($item.Attributes -band [IO.FileAttributes]::ReparsePoint)) { throw 'Invalid credential file.' }
    }
    return $full
}

function Set-Stage2PrivateAcl([string]$Path, [bool]$Directory) {
    $owner = [Security.Principal.WindowsIdentity]::GetCurrent().User
    $system = New-Object Security.Principal.SecurityIdentifier('S-1-5-18')
    # Modify the owner/DACL only. Use the filesystem ACL API below rather than
    # Set-Acl, which can request SeSecurityPrivilege on an existing protected file.
    $acl = Get-Acl -LiteralPath $Path -ErrorAction Stop
    $acl.SetAccessRuleProtection($true, $false)
    foreach ($existing in @($acl.GetAccessRules($true, $false, [Security.Principal.SecurityIdentifier]))) {
        $acl.RemoveAccessRuleSpecific($existing)
    }
    $acl.SetOwner($owner)
    $acl.SetAccessRuleProtection($true, $false)
    foreach ($identity in @($owner, $system)) {
        if ($Directory) {
            $rule = New-Object Security.AccessControl.FileSystemAccessRule($identity, 'FullControl', 'ContainerInherit,ObjectInherit', 'None', 'Allow')
        } else { $rule = New-Object Security.AccessControl.FileSystemAccessRule($identity, 'FullControl', 'Allow') }
        $acl.AddAccessRule($rule)
    }
    $item = Get-Item -LiteralPath $Path -Force -ErrorAction Stop
    if ($PSVersionTable.PSEdition -eq 'Core') {
        [IO.FileSystemAclExtensions]::SetAccessControl($item, $acl)
    } else {
        $item.SetAccessControl($acl)
    }
}

function Save-Stage2GeminiCredential([Security.SecureString]$Secret, [string]$Directory = (Get-Stage2CredentialDirectory)) {
    $temporary = $null
    $protected = $null
    try {
        $Directory = Assert-Stage2CredentialPath $Directory
        if ($null -eq $Secret -or $Secret.Length -eq 0) { throw 'Empty credential.' }
        New-Item -ItemType Directory -Path $Directory -Force -ErrorAction Stop | Out-Null
        Set-Stage2PrivateAcl $Directory $true
        $file = Join-Path $Directory 'gemini-api-key.dpapi'
        if (Test-Path -LiteralPath $file) { Set-Stage2PrivateAcl $file $false }
        # No -Key/-SecureKey: Windows PowerShell uses current-user DPAPI, never a custom password.
        $protected = ConvertFrom-SecureString -SecureString $Secret -ErrorAction Stop
        $temporary = Join-Path $Directory ('gemini-' + [guid]::NewGuid().ToString('N') + '.dpapi.tmp')
        [IO.File]::WriteAllText($temporary, $protected, [Text.UTF8Encoding]::new($false))
        Set-Stage2PrivateAcl $temporary $false
        if (Test-Path -LiteralPath $file) { [IO.File]::Replace($temporary, $file, [NullString]::Value) }
        else { [IO.File]::Move($temporary, $file) }
        $temporary = $null
    } catch { throw 'Encrypted Gemini enrollment failed. Check local storage permissions and retry; existing enrollment is retained unless replacement completed.' }
    finally {
        $protected = $null
        if ($temporary -and (Test-Path -LiteralPath $temporary)) { Remove-Item -LiteralPath $temporary -Force -ErrorAction SilentlyContinue }
    }
}

function Read-Stage2GeminiCredential([string]$Directory = (Get-Stage2CredentialDirectory)) {
    try {
        $Directory = Assert-Stage2CredentialPath $Directory
        $file = Join-Path $Directory 'gemini-api-key.dpapi'
        if (-not (Test-Path -LiteralPath $file)) { return $null }
        if ((Get-Item -LiteralPath $file -ErrorAction Stop).Length -gt 65536) { throw 'Oversized file.' }
        $protected = [IO.File]::ReadAllText($file)
        return ConvertTo-SecureString -String $protected -ErrorAction Stop
    } catch { throw 'Saved Gemini credential could not be decrypted or read. Run setup-gemini.ps1 to replace it.' }
    finally { $protected = $null }
}

function Remove-Stage2GeminiCredential([string]$Directory = (Get-Stage2CredentialDirectory)) {
    try {
        $Directory = Assert-Stage2CredentialPath $Directory
        $file = Join-Path $Directory 'gemini-api-key.dpapi'
        if (Test-Path -LiteralPath $file) { Remove-Item -LiteralPath $file -Force -ErrorAction Stop }
    } catch { throw 'Saved Gemini credential could not be removed. Check local storage permissions.' }
}

function Invoke-WithStage2Gemini {
    param([Parameter(Mandatory=$true)][scriptblock]$Action, [string]$Directory)
    $temporaryEnvironment = $false
    $secret = $null
    $plain = $null
    $pointer = [IntPtr]::Zero
    try {
        # Explicit process input wins, including invalid nonempty input; Java performs configuration validation.
        if ([string]::IsNullOrEmpty([Environment]::GetEnvironmentVariable('GEMINI_API_KEY', 'Process'))) {
            try {
                if (-not $Directory) { $Directory = Get-Stage2CredentialDirectory }
                $secret = Read-Stage2GeminiCredential $Directory
                if ($null -ne $secret) {
                    $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secret)
                    $plain = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
                    $temporaryEnvironment = $true
                    [Environment]::SetEnvironmentVariable('GEMINI_API_KEY', $plain, 'Process')
                }
            } catch {
                if ($temporaryEnvironment) { [Environment]::SetEnvironmentVariable('GEMINI_API_KEY', $null, 'Process') }
                Write-Warning 'Saved Gemini credential unavailable. Deterministic mode remains usable; run .\scripts\setup-gemini.ps1 to enroll or replace it.'
            } finally {
                if ($pointer -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer); $pointer = [IntPtr]::Zero }
                $plain = $null
                if ($null -ne $secret) { $secret.Dispose(); $secret = $null }
            }
            if ([string]::IsNullOrEmpty([Environment]::GetEnvironmentVariable('GEMINI_API_KEY', 'Process'))) {
                Write-Host 'Gemini NOT CONFIGURED. Optional one-time setup: powershell -NoProfile -ExecutionPolicy Bypass -File .\scripts\setup-gemini.ps1'
            }
        }
        & $Action
    } finally {
        if ($temporaryEnvironment) { [Environment]::SetEnvironmentVariable('GEMINI_API_KEY', $null, 'Process') }
        if ($pointer -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
        if ($null -ne $secret) { $secret.Dispose() }
        $plain = $null; $secret = $null; $pointer = [IntPtr]::Zero
    }
}

function Invoke-Stage2Java {
    param([string]$Java, [string]$Jar, [ValidateSet('demo','cli','gui','help')][string]$Mode)
    if ($Mode -eq 'gui') {
        # No secrets in arguments. The Process wait covers the actual Java lifetime, not just Swing main().
        $process = $null
        try {
            $arguments = '-Dfile.encoding=UTF-8 -jar "' + $Jar + '" --gui'
            $process = Start-Process -FilePath $Java -ArgumentList $arguments -WindowStyle Hidden -PassThru -ErrorAction Stop
            $process.WaitForExit()
            if ($process.ExitCode -ne 0) { throw 'Java GUI exited unsuccessfully.' }
        } catch { throw 'Java GUI could not start or exited unsuccessfully. Temporary launcher credentials have been scheduled for cleanup.' }
        finally { if ($null -ne $process) { $process.Dispose() } }
    } else {
        & $Java '-Dfile.encoding=UTF-8' -jar $Jar "--$Mode"
        if ($LASTEXITCODE -ne 0) { throw 'Java application exited unsuccessfully.' }
    }
}
