[CmdletBinding(SupportsShouldProcess=$true, ConfirmImpact='High')]
param()
$ErrorActionPreference = 'Stop'
. (Join-Path $PSScriptRoot 'gemini-credentials.ps1')
try {
    if ($PSCmdlet.ShouldProcess('Saved encrypted Gemini credential for this Windows user', 'Remove local credential (does not revoke the Google key)')) {
        Remove-Stage2GeminiCredential
        Write-Host 'Saved credential removed (or already absent). The Google key was not revoked; explicit environment variables are unaffected.'
    }
} catch { Write-Error 'Saved credential could not be removed. Check Windows local storage permissions.' }
