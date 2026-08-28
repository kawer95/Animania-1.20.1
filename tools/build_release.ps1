[CmdletBinding()]
param(
    [ValidateSet('base', 'farm', 'extra', 'catsdogs', 'config-migrator')]
    [string]$FocusedModule,

    [string]$DeliveryDirectory,

    [switch]$ValidateOnly
)

$ErrorActionPreference = 'Stop'
$repoRoot = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$gradleWrapper = Join-Path $repoRoot 'gradlew.bat'

if ([string]::IsNullOrWhiteSpace($DeliveryDirectory)) {
    $DeliveryDirectory = [IO.Directory]::GetParent($repoRoot).FullName
}
$deliveryRoot = [IO.Path]::GetFullPath($DeliveryDirectory)

function Write-Step([string]$Message) {
    Write-Host "`n==> $Message" -ForegroundColor Cyan
}

function Invoke-Checked([string]$Executable, [string[]]$Arguments) {
    & $Executable @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "Command failed with exit code $LASTEXITCODE`: $Executable $($Arguments -join ' ')"
    }
}

function Enable-RealPython {
    $candidates = [Collections.Generic.List[string]]::new()
    $launcher = Get-Command py.exe -ErrorAction SilentlyContinue
    if ($launcher) {
        $previousPreference = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        $launcherPath = if ($launcher.Path) { $launcher.Path } else { $launcher.Source }
        $launcherOutput = @(& $launcherPath -3 -c 'import sys; print(sys.executable)' 2>$null)
        $launcherExitCode = $LASTEXITCODE
        $ErrorActionPreference = $previousPreference
        if ($launcherExitCode -eq 0 -and $launcherOutput.Count -gt 0) {
            $launchedPython = $launcherOutput[0].ToString().Trim()
            if (-not [string]::IsNullOrWhiteSpace($launchedPython)) {
                $candidates.Add($launchedPython)
            }
        }
    }

    $localPythonRoot = Join-Path $env:LOCALAPPDATA 'Programs\Python'
    if ([IO.Directory]::Exists($localPythonRoot)) {
        Get-ChildItem -LiteralPath $localPythonRoot -Directory -Filter 'Python*' |
            ForEach-Object { $candidates.Add((Join-Path $_.FullName 'python.exe')) }
    }

    Get-Command python.exe -All -ErrorAction SilentlyContinue |
        Where-Object { $_.Source -notmatch '\\WindowsApps\\' } |
        ForEach-Object { $candidates.Add($_.Source) }

    foreach ($candidate in $candidates | Select-Object -Unique) {
        if (-not [IO.File]::Exists($candidate)) { continue }
        $previousPreference = $ErrorActionPreference
        $ErrorActionPreference = 'Continue'
        & $candidate -c 'import sys; print(sys.version_info.major)' *> $null
        $pythonExitCode = $LASTEXITCODE
        $ErrorActionPreference = $previousPreference
        if ($pythonExitCode -ne 0) { continue }

        $pythonDirectory = [IO.Path]::GetDirectoryName($candidate)
        $env:Path = "$pythonDirectory;$env:Path"
        $version = (& $candidate --version 2>&1 | Select-Object -First 1)
        Write-Host "Python: $version ($candidate)"
        return
    }

    throw 'No working Python 3 interpreter was found. Install Python 3 or make the py launcher available.'
}

function Assert-Java17 {
    $java = Get-Command java.exe -ErrorAction SilentlyContinue
    if (-not $java) { throw 'Java was not found on PATH. This project requires JDK 17.' }

    # Java 17's long option writes its version to stdout, unlike `-version`,
    # which Windows PowerShell 5.1 can promote to a NativeCommandError.
    $versionText = (& $java.Source --version | Out-String)
    $javaExitCode = $LASTEXITCODE
    if ($javaExitCode -ne 0 -or $versionText -notmatch '(?m)^(?:java|openjdk) 17(?:\.|\s)') {
        throw "JDK 17 is required, but the active Java is:`n$versionText"
    }
    Write-Host "Java: $(($versionText -split "`r?`n")[0]) ($($java.Source))"
}

if (-not [IO.File]::Exists($gradleWrapper)) {
    throw "Gradle wrapper not found: $gradleWrapper"
}
if (-not [IO.File]::Exists((Join-Path $repoRoot 'settings.gradle'))) {
    throw "Not an Animania repository root: $repoRoot"
}

Write-Step 'Checking the release environment'
Write-Host "Repository: $repoRoot"
Write-Host "Delivery:   $deliveryRoot"
Assert-Java17
Enable-RealPython

if ($ValidateOnly) {
    Write-Host "`nEnvironment validation passed; no tests, build, or file copies were performed." -ForegroundColor Green
    exit 0
}

$artifacts = @(
    [pscustomobject]@{ Module = 'base'; Name = 'animania-base-1.20.1-3.0.0.jar' },
    [pscustomobject]@{ Module = 'farm'; Name = 'animania-farm-1.20.1-3.0.0.jar' },
    [pscustomobject]@{ Module = 'extra'; Name = 'animania-extra-1.20.1-3.0.0.jar' },
    [pscustomobject]@{ Module = 'catsdogs'; Name = 'animania-catsdogs-1.20.1-3.0.0.jar' }
)

Push-Location $repoRoot
try {
    if ($FocusedModule) {
        Write-Step "Running focused tests for $FocusedModule"
        Invoke-Checked $gradleWrapper @("`:$FocusedModule`:test", '--console=plain')
    }

    Write-Step 'Running all Java unit tests'
    Invoke-Checked $gradleWrapper @('test', '--console=plain')

    Write-Step 'Building and auditing production JARs once'
    Invoke-Checked $gradleWrapper @('releaseBuild', '--console=plain')

    Write-Step 'Checking the working diff for whitespace errors'
    Invoke-Checked 'git.exe' @('diff', '--check')

    Write-Step 'Copying verified production JARs'
    [IO.Directory]::CreateDirectory($deliveryRoot) | Out-Null
    $results = foreach ($artifact in $artifacts) {
        $source = Join-Path $repoRoot "$($artifact.Module)\build\release\$($artifact.Name)"
        if (-not [IO.File]::Exists($source)) {
            throw "Missing production JAR after releaseBuild: $source"
        }

        $target = Join-Path $deliveryRoot $artifact.Name
        $sourceHash = (Get-FileHash -LiteralPath $source -Algorithm SHA256).Hash
        Copy-Item -LiteralPath $source -Destination $target -Force
        $targetHash = (Get-FileHash -LiteralPath $target -Algorithm SHA256).Hash
        if ($sourceHash -ne $targetHash) {
            throw "SHA-256 mismatch after copying $($artifact.Name)"
        }

        $targetInfo = Get-Item -LiteralPath $target
        [pscustomobject]@{
            Module = $artifact.Module
            File = $targetInfo.FullName
            Bytes = $targetInfo.Length
            SHA256 = $targetHash
        }
    }

    Write-Step 'Release complete'
    $results | Format-Table Module, Bytes, SHA256, File -AutoSize
} finally {
    Pop-Location
}
