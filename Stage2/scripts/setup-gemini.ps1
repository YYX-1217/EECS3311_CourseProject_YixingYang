[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'gemini-credentials.ps1')
$secret = $null
try {
    $directory = Assert-Stage2CredentialPath (Get-Stage2CredentialDirectory)
    if (Test-Path -LiteralPath (Join-Path $directory 'gemini-api-key.dpapi')) {
        if ((Read-Host 'Replace the existing encrypted Gemini credential? Type YES to continue') -cne 'YES') {
            Write-Host 'Enrollment cancelled; saved credential unchanged.'
            return
        }
    }
    $secret = Read-Host 'Gemini API key (hidden; saved for this Windows user only)' -AsSecureString
    Save-Stage2GeminiCredential -Secret $secret -Directory $directory
    Write-Host 'Encrypted Gemini enrollment succeeded. Local storage does not verify Google authorization.'
} catch { Write-Error 'Encrypted Gemini enrollment failed. Check Windows local storage permissions and retry. No secret material is displayed.' }
finally { if ($null -ne $secret) { $secret.Dispose(); $secret = $null } }
