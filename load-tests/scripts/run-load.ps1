param(
  [string]$BaseUrl,
  [string]$Email,
  [string]$Password
)

$ErrorActionPreference = "Stop"
$Root = Split-Path -Parent $PSScriptRoot

. (Join-Path $PSScriptRoot "_load-env.ps1") -LoadTestsRoot $Root

if (-not $BaseUrl) {
  $BaseUrl = if ($env:BASE_URL) { $env:BASE_URL } else { "http://localhost:8085/api/v1" }
}
if ($Email) { $env:EMAIL = $Email }
if ($Password) { $env:PASSWORD = $Password }
$env:BASE_URL = $BaseUrl

if (-not $env:EMAIL -or -not $env:PASSWORD) {
  Write-Error "EMAIL and PASSWORD are required. Copy load-tests/.env.example to load-tests/.env or set env vars."
}

Write-Host "Running k6 load test against $BaseUrl (EMAIL=$env:EMAIL)"
Push-Location $Root
try {
  k6 run load.js
  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally {
  Pop-Location
}
