[CmdletBinding()]
param(
    [string]$Sclang = "",

    [Alias("Check")]
    [switch]$CheckOnly,

    [int]$ReadyTimeoutSeconds = 90
)

# ASCII only: Windows PowerShell 5.1 reads a BOM-less script as ANSI.
$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$appScript = Join-Path $PSScriptRoot "yonpuoli_app.scd"
$appClasses = Join-Path $PSScriptRoot "app_classes"
$configExample = Join-Path $repoRoot "config.example.scd"
$configPath = Join-Path $repoRoot "config.scd"
$token = [Guid]::NewGuid().ToString("N")
$tempRoot = [System.IO.Path]::GetTempPath()
$classConfigPath = Join-Path $tempRoot "yonpuoli-classes-$token.yaml"
$readyPath = Join-Path $tempRoot "yonpuoli-ready-$token.txt"
$process = $null

function Resolve-Sclang {
    param([string]$Explicit)

    if (-not [string]::IsNullOrWhiteSpace($Explicit)) {
        if (Test-Path -LiteralPath $Explicit -PathType Leaf) {
            return (Resolve-Path -LiteralPath $Explicit).Path
        }
        throw "sclang.exe was not found at the requested path: $Explicit"
    }

    if (-not [string]::IsNullOrWhiteSpace($env:SC_LANG)) {
        if (Test-Path -LiteralPath $env:SC_LANG -PathType Leaf) {
            return (Resolve-Path -LiteralPath $env:SC_LANG).Path
        }
        throw "SC_LANG points to a missing file: $env:SC_LANG"
    }

    $roots = @((Join-Path $env:ProgramFiles "SuperCollider-*"))
    if (${env:ProgramFiles(x86)}) {
        $roots += (Join-Path ${env:ProgramFiles(x86)} "SuperCollider-*")
    }

    $found = Get-ChildItem -Path $roots -Directory -ErrorAction SilentlyContinue |
        ForEach-Object {
            $exe = Join-Path $_.FullName "sclang.exe"
            if (Test-Path -LiteralPath $exe -PathType Leaf) {
                $version = [version]"0.0"
                if ($_.Name -match "SuperCollider-(\d+(\.\d+)*)") {
                    try { $version = [version]$Matches[1] } catch { }
                }
                [pscustomobject]@{ Path = $exe; Version = $version }
            }
        } |
        Sort-Object -Property Version -Descending |
        Select-Object -First 1

    if ($found) { return $found.Path }
    throw ("SuperCollider was not found. Install SuperCollider 3.13 or newer " +
           "from https://supercollider.github.io/downloads and try again.")
}

function Get-FreeUdpPort {
    $socket = [System.Net.Sockets.UdpClient]::new(0)
    try {
        return ([System.Net.IPEndPoint]$socket.Client.LocalEndPoint).Port
    } finally {
        $socket.Dispose()
    }
}

function Get-PrivatePortPair {
    $language = Get-FreeUdpPort
    do {
        $audioServer = Get-FreeUdpPort
    } while ($audioServer -eq $language)
    return [pscustomobject]@{
        Language = $language
        AudioServer = $audioServer
    }
}

function Stop-OwnedProcessTree {
    param([System.Diagnostics.Process]$OwnedProcess)

    if ($null -eq $OwnedProcess) { return }
    try {
        if (-not $OwnedProcess.HasExited) {
            $taskkill = Join-Path $env:SystemRoot "System32\taskkill.exe"
            & $taskkill /PID $OwnedProcess.Id /T /F 2>$null | Out-Null
            $OwnedProcess.WaitForExit()
        }
    } catch {
        # Last resort for an unusual host without taskkill. The exact process
        # object was created by this launcher; never search by image name.
        try {
            if (-not $OwnedProcess.HasExited) {
                $OwnedProcess.Kill()
                $OwnedProcess.WaitForExit()
            }
        } catch { }
    }
}

function Write-ClassConfig {
    param(
        [string]$Destination,
        [string]$Language,
        [string]$ProjectClasses,
        [string]$ApplicationClasses
    )

    $classLibrary = Join-Path (Split-Path -Parent $Language) "SCClassLibrary"
    foreach ($path in @($classLibrary, $ProjectClasses, $ApplicationClasses)) {
        if (-not (Test-Path -LiteralPath $path -PathType Container)) {
            throw "Required class directory was not found: $path"
        }
        if ($path.Contains('"')) {
            throw "A class directory contains an unsupported quote: $path"
        }
    }

    $paths = @($classLibrary, $ProjectClasses, $ApplicationClasses) |
        ForEach-Object { $_.Replace("\", "/") }
    $yaml = @(
        "includePaths:"
        "    - `"$($paths[0])`""
        "    - `"$($paths[1])`""
        "    - `"$($paths[2])`""
        "excludeDefaultPaths: true"
    ) -join [Environment]::NewLine
    [System.IO.File]::WriteAllText(
        $Destination,
        $yaml,
        [System.Text.UTF8Encoding]::new($false)
    )
}

try {
    if ($ReadyTimeoutSeconds -lt 10) {
        throw "ReadyTimeoutSeconds must be at least 10."
    }

    $Sclang = Resolve-Sclang $Sclang
    $projectClasses = Join-Path $repoRoot "classes"
    foreach ($path in @($Sclang, $appScript, $appClasses, $configExample,
                        $projectClasses)) {
        if (-not (Test-Path -LiteralPath $path)) {
            throw "YONPUOLI launch dependency was not found: $path"
        }
    }

    Write-ClassConfig $classConfigPath $Sclang $projectClasses $appClasses
    $portPair = Get-PrivatePortPair

    if ($CheckOnly) {
        if (-not (Test-Path -LiteralPath $classConfigPath -PathType Leaf)) {
            throw "The isolated SuperCollider class configuration was not created."
        }
        Write-Host "sclang: $Sclang"
        Write-Host "project: $repoRoot"
        Write-Host "private ports: language $($portPair.Language), audio $($portPair.AudioServer)"
        Write-Host "YONPUOLI_LAUNCHER_CHECK_OK"
        exit 0
    }

    if (-not (Test-Path -LiteralPath $configPath -PathType Leaf)) {
        Copy-Item -LiteralPath $configExample -Destination $configPath
        Write-Host "Created local config.scd from config.example.scd."
    }
    foreach ($folder in @("samples", "recordings", "presets", "snapshots")) {
        [System.IO.Directory]::CreateDirectory((Join-Path $repoRoot $folder)) |
            Out-Null
    }

    $languagePort = $portPair.Language
    $serverPort = $portPair.AudioServer
    $env:YONPUOLI_READY_FILE = $readyPath
    $env:YONPUOLI_SERVER_PORT = $serverPort.ToString()

    $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName = $Sclang
    $startInfo.Arguments =
        "-D -a -l `"$classConfigPath`" -i none -u $languagePort `"$appScript`""
    $startInfo.WorkingDirectory = $repoRoot
    $startInfo.UseShellExecute = $false
    # Share the launcher's diagnostic console. If the console itself is closed,
    # Windows can terminate the attached process group instead of leaving a
    # detached sclang/scsynth pair behind.
    $startInfo.CreateNoWindow = $false

    Write-Host "Starting YONPUOLI with: $Sclang"
    Write-Host "Private audio server port: $serverPort"
    Write-Host "Keep this window open while the instrument is running."
    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo = $startInfo
    [void]$process.Start()

    $deadline = [DateTime]::UtcNow.AddSeconds($ReadyTimeoutSeconds)
    $startedAt = [DateTime]::UtcNow
    $nextProgress = $startedAt.AddSeconds(5)
    Write-Host "Waiting for audio and the Night window (up to ${ReadyTimeoutSeconds}s)..."
    while (-not $process.HasExited -and
           -not (Test-Path -LiteralPath $readyPath -PathType Leaf) -and
           [DateTime]::UtcNow -lt $deadline) {
        [System.Threading.Thread]::Sleep(200)
        if ([DateTime]::UtcNow -ge $nextProgress) {
            $elapsed = [int]([DateTime]::UtcNow - $startedAt).TotalSeconds
            Write-Host "Still starting YONPUOLI... ${elapsed}s"
            $nextProgress = $nextProgress.AddSeconds(5)
        }
    }

    if (-not (Test-Path -LiteralPath $readyPath -PathType Leaf)) {
        if (-not $process.HasExited) {
            Stop-OwnedProcessTree $process
            throw "YONPUOLI did not become ready within ${ReadyTimeoutSeconds}s."
        }
        throw "YONPUOLI stopped before its window became ready (exit $($process.ExitCode))."
    }

    $ready = (Get-Content -LiteralPath $readyPath -Raw).Trim()
    if ($ready -ne "YONPUOLI_APP_READY") {
        throw "YONPUOLI returned an invalid ready marker: $ready"
    }

    Write-Host "YONPUOLI is ready. Close its window to stop audio cleanly."
    $process.WaitForExit()
    if ($process.ExitCode -ne 0) {
        throw "YONPUOLI stopped with exit code $($process.ExitCode)."
    }
} catch {
    [Console]::Error.WriteLine("")
    [Console]::Error.WriteLine("YONPUOLI launcher error:")
    [Console]::Error.WriteLine($_.Exception.Message)
    exit 1
} finally {
    Stop-OwnedProcessTree $process
    Remove-Item -LiteralPath $classConfigPath -Force -ErrorAction SilentlyContinue
    Remove-Item -LiteralPath $readyPath -Force -ErrorAction SilentlyContinue
    Remove-Item Env:YONPUOLI_READY_FILE -ErrorAction SilentlyContinue
    Remove-Item Env:YONPUOLI_SERVER_PORT -ErrorAction SilentlyContinue
}

exit 0
