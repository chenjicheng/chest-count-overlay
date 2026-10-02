[CmdletBinding()]
param([switch]$AcceptMinecraftEula)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
if (-not $AcceptMinecraftEula) {
    throw 'Run with -AcceptMinecraftEula only after accepting https://aka.ms/MinecraftEULA.'
}

$projectRoot = Split-Path -Parent $PSScriptRoot
$jdk = Get-ChildItem -LiteralPath 'C:\Program Files\Eclipse Adoptium' -Directory -Filter 'jdk-21*' |
    Sort-Object Name -Descending | Select-Object -First 1
if ($null -ne $jdk) {
    $env:JAVA_HOME = $jdk.FullName
    $env:Path = "$($jdk.FullName)\bin;$env:Path"
}

Push-Location $projectRoot
try {
    $completion = Join-Path $projectRoot 'build/run/clientGameTest/documentation-screenshots/completed.json'
    if (Test-Path -LiteralPath $completion) { Remove-Item -LiteralPath $completion }
    & .\gradlew.bat runClientGameTest -PacceptMinecraftEula --no-daemon --console=plain
    if ($LASTEXITCODE -ne 0) { throw 'Client screenshot tests failed; documentation images were not changed.' }
    & python scripts/collect_screenshots.py
    if ($LASTEXITCODE -ne 0) { throw 'Screenshot collection failed.' }
} finally {
    Pop-Location
}
