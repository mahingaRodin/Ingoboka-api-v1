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
if ($Email) { $env:EMAIL = $Email } elseif (-not $env:EMAIL) { $env:EMAIL = "agressive.one04@gmail.com" }
if ($Password) { $env:PASSWORD = $Password } elseif (-not $env:PASSWORD) { $env:PASSWORD = "Olga!132" }
$env:BASE_URL = $BaseUrl

Write-Host "Running k6 stress test against $BaseUrl"
Push-Location $Root
try {
  k6 run stress.js
  if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
} finally {
  Pop-Location
}
