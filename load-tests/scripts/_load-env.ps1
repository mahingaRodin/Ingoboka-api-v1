# Dot-source from run-*.ps1 to load load-tests/.env without overwriting existing env vars.
param(
  [Parameter(Mandatory = $true)]
  [string]$LoadTestsRoot
)

$envFile = Join-Path $LoadTestsRoot ".env"
if (-not (Test-Path $envFile)) {
  return
}

Get-Content $envFile | ForEach-Object {
  $line = $_.Trim()
  if ($line -eq "" -or $line.StartsWith("#")) {
    return
  }
  if ($line -match '^\s*([^#=]+)=(.*)$') {
    $name = $matches[1].Trim()
    $value = $matches[2].Trim().Trim('"').Trim("'")
    if (-not (Get-Item "Env:$name" -ErrorAction SilentlyContinue)) {
      Set-Item -Path "Env:$name" -Value $value
    }
  }
}
