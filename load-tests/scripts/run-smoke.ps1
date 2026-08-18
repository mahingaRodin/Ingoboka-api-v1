param(
  [string]$BaseUrl,
  [string]$Email,
  [string]$Password
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot

if (-not $BaseUrl) {
  $BaseUrl = if ($env:BASE_URL) { $env:BASE_URL } else { "http://localhost:8085/api/v1" }
}
$usingDefaultEmail = -not $Email -and -not $env:EMAIL
$usingDefaultPassword = -not $Password -and -not $env:PASSWORD
if ($Email) { $env:EMAIL = $Email } elseif (-not $env:EMAIL) { $env:EMAIL = "agressive.one04@gmail.com" }
if ($Password) { $env:PASSWORD = $Password } elseif (-not $env:PASSWORD) { $env:PASSWORD = "admin@123" }
$env:BASE_URL = $BaseUrl

$isRemote = $BaseUrl -notmatch 'localhost|127\.0\.0\.1'
if ($isRemote -and ($usingDefaultEmail -or $usingDefaultPassword)) {
  Write-Warning @"
Remote BASE_URL detected but default login credentials are in use.
On the Azure demo VM, platform admin password is NOT admin@123 — login will fail 100%.
Set known-working credentials before running, e.g.:
  `$env:EMAIL = "eric@demo-insurer.rw"
  `$env:PASSWORD = "Ingoboka@2026"
"@
}

Write-Host "Running k6 smoke against $BaseUrl (EMAIL=$env:EMAIL)"
Push-Location $Root
try {
  k6 run smoke.js
  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally {
  Pop-Location
}
