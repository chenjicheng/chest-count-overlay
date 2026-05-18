[CmdletBinding()]
param(
    [switch]$SkipBuild,
    [switch]$RefreshDependencies
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'

$ProjectRoot = Split-Path -Parent $PSScriptRoot
$GradleWrapper = Join-Path $ProjectRoot 'gradlew.bat'

function Write-Step {
    param([Parameter(Mandatory = $true)][string]$Message)
    Write-Host "==> $Message"
}

function Update-ProcessPath {
    $userPath = [Environment]::GetEnvironmentVariable('Path', 'User')
    $machinePath = [Environment]::GetEnvironmentVariable('Path', 'Machine')
    $env:Path = "$userPath;$machinePath"

    $temurinRoot = 'C:\Program Files\Eclipse Adoptium'
    if (Test-Path -LiteralPath $temurinRoot) {
        $jdk21 = Get-ChildItem -LiteralPath $temurinRoot -Directory -Filter 'jdk-21*' |
            Sort-Object Name -Descending |
            Select-Object -First 1

        if ($null -ne $jdk21) {
            $env:JAVA_HOME = $jdk21.FullName
            $env:Path = "$($jdk21.FullName)\bin;$env:Path"
        }
    }
}

function Get-JavaMajorVersion {
    if ($null -eq (Get-Command java -ErrorAction SilentlyContinue)) {
        return $null
    }

    $javaSettings = (& cmd /c 'java -XshowSettings:properties -version 2>&1') -join "`n"
    if ($javaSettings -notmatch 'java\.version\s*=\s*([^\s]+)') {
        return $null
    }

    $version = $Matches[1]
    if ($version.StartsWith('1.')) {
        return [int]($version.Split('.')[1])
    }

    return [int](($version -split '[.+_-]')[0])
}

function Install-Temurin21 {
    if ($null -ne (Get-Command winget -ErrorAction SilentlyContinue)) {
        Write-Step 'Installing Temurin 21 through winget'
        & winget install --id EclipseAdoptium.Temurin.21.JDK --source winget --accept-package-agreements --accept-source-agreements
        if ($LASTEXITCODE -ne 0) {
            throw "winget failed to install Temurin 21. Exit code: $LASTEXITCODE"
        }
        return
    }

    if ($null -ne (Get-Command choco -ErrorAction SilentlyContinue)) {
        Write-Step 'Installing Temurin 21 through Chocolatey'
        & choco install Temurin21 -y
        if ($LASTEXITCODE -ne 0) {
            throw "Chocolatey failed to install Temurin 21. Exit code: $LASTEXITCODE"
        }
        return
    }

    throw 'No supported package manager found. Install winget or Chocolatey, then rerun this script.'
}

Push-Location $ProjectRoot
try {
    Write-Step 'Checking Java'
    Update-ProcessPath
    $javaMajor = Get-JavaMajorVersion
    if ($javaMajor -ne 21) {
        if ($null -eq $javaMajor) {
            Write-Step 'Java was not found'
        } else {
            Write-Step "Found Java $javaMajor, but this project requires Java 21"
        }

        Install-Temurin21
        Update-ProcessPath
        $javaMajor = Get-JavaMajorVersion
    }

    if ($javaMajor -ne 21) {
        throw "Java 21 is still unavailable after package-manager installation. Current detected major version: $javaMajor"
    }

    & cmd /c 'java -version'
    if ($LASTEXITCODE -ne 0) {
        throw "java -version failed. Exit code: $LASTEXITCODE"
    }

    if (-not (Test-Path -LiteralPath $GradleWrapper)) {
        throw 'Gradle Wrapper is missing. This project must use .\gradlew.bat instead of a global Gradle install.'
    }

    Write-Step 'Checking Gradle Wrapper'
    & $GradleWrapper --version
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle Wrapper check failed. Exit code: $LASTEXITCODE"
    }

    if ($SkipBuild) {
        Write-Step 'Skipping build by request'
        return
    }

    $gradleArgs = @()
    if ($RefreshDependencies) {
        $gradleArgs += '--refresh-dependencies'
    }
    $gradleArgs += 'build'

    Write-Step "Running .\gradlew.bat $($gradleArgs -join ' ')"
    & $GradleWrapper @gradleArgs
    if ($LASTEXITCODE -ne 0) {
        throw "Gradle build failed. Exit code: $LASTEXITCODE"
    }
} finally {
    Pop-Location
}
