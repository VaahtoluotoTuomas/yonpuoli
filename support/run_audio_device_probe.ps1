param(
    [Parameter(Mandatory = $true)]
    [string]$OutputPath,

    [string]$Sclang = "",

    [int]$TimeoutSeconds = 20,

    [int]$Passes = 1,

    [int]$SettleMilliseconds = 750
)

# ASCII only: Windows PowerShell 5.1 reads a BOM-less script as ANSI.
$ErrorActionPreference = 'Stop'
$probeScript = Join-Path $PSScriptRoot 'audio_device_probe.scd'
$probeClasses = Join-Path $PSScriptRoot 'audio_probe_classes'
$tempRoot = [System.IO.Path]::GetTempPath()
$token = [Guid]::NewGuid().ToString('N')
$configPath = Join-Path $tempRoot "cq-audio-probe-$token.yaml"
$logPath = Join-Path $tempRoot "cq-audio-probe-$token.log"

function Resolve-Sclang {
    param([string]$Explicit)

    if (-not [string]::IsNullOrWhiteSpace($Explicit)) {
        if (Test-Path -LiteralPath $Explicit) {
            return (Resolve-Path -LiteralPath $Explicit).Path
        }
        throw "sclang not found: $Explicit"
    }
    if (-not [string]::IsNullOrWhiteSpace($env:SC_LANG) -and
        (Test-Path -LiteralPath $env:SC_LANG)) {
        return (Resolve-Path -LiteralPath $env:SC_LANG).Path
    }

    $roots = @((Join-Path $env:ProgramFiles 'SuperCollider-*'))
    if (${env:ProgramFiles(x86)}) {
        $roots += (Join-Path ${env:ProgramFiles(x86)} 'SuperCollider-*')
    }
    $found = Get-ChildItem -Path $roots -Directory -ErrorAction SilentlyContinue |
        ForEach-Object {
            $exe = Join-Path $_.FullName 'sclang.exe'
            if (Test-Path -LiteralPath $exe) {
                $version = [version]'0.0'
                if ($_.Name -match 'SuperCollider-(\d+(\.\d+)*)') {
                    try { $version = [version]$Matches[1] } catch { }
                }
                [pscustomobject]@{ Path = $exe; Version = $version }
            }
        } |
        Sort-Object Version -Descending |
        Select-Object -First 1
    if ($found) { return $found.Path }
    throw 'sclang.exe could not be resolved'
}

function Get-FreeUdpPort {
    $socket = [System.Net.Sockets.UdpClient]::new(0)
    try {
        return ([System.Net.IPEndPoint]$socket.Client.LocalEndPoint).Port
    } finally {
        $socket.Dispose()
    }
}

try {
    if ($Passes -lt 1 -or $Passes -gt 5) {
        throw 'Passes must be between 1 and 5'
    }
    if ($SettleMilliseconds -lt 0 -or $SettleMilliseconds -gt 5000) {
        throw 'SettleMilliseconds must be between 0 and 5000'
    }
    $Sclang = Resolve-Sclang $Sclang
    $installRoot = Split-Path -Parent $Sclang
    $classLibrary = Join-Path $installRoot 'SCClassLibrary'
    foreach ($path in @($Sclang, $classLibrary, $probeScript, $probeClasses)) {
        if (-not (Test-Path -LiteralPath $path)) {
            throw "audio probe dependency not found: $path"
        }
    }

    $classYaml = $classLibrary.Replace('\', '/')
    $probeYaml = $probeClasses.Replace('\', '/')
    $yaml = @(
        'includePaths:'
        "    - `"$classYaml`""
        "    - `"$probeYaml`""
        'excludeDefaultPaths: true'
    ) -join [Environment]::NewLine
    [System.IO.File]::WriteAllText(
        $configPath,
        $yaml,
        [System.Text.UTF8Encoding]::new($false)
    )

    $env:CQ_AUDIO_PROBE_OUTPUT = $OutputPath
    $combinedLog = [System.Text.StringBuilder]::new()
    for ($pass = 1; $pass -le $Passes; $pass++) {
        if ($pass -gt 1 -and $SettleMilliseconds -gt 0) {
            Start-Sleep -Milliseconds $SettleMilliseconds
        }
        if (Test-Path -LiteralPath $OutputPath) {
            Remove-Item -LiteralPath $OutputPath -Force
        }

        $port = Get-FreeUdpPort
        $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
        $startInfo.FileName = $Sclang
        $startInfo.Arguments =
            "-D -a -l `"$configPath`" -i none -u $port `"$probeScript`""
        $startInfo.WorkingDirectory = Split-Path -Parent $PSScriptRoot
        $startInfo.UseShellExecute = $false
        $startInfo.CreateNoWindow = $true
        $startInfo.RedirectStandardOutput = $true
        $startInfo.RedirectStandardError = $true

        $process = [System.Diagnostics.Process]::new()
        $process.StartInfo = $startInfo
        [void]$process.Start()
        $stdout = $process.StandardOutput.ReadToEndAsync()
        $stderr = $process.StandardError.ReadToEndAsync()

        if (-not $process.WaitForExit($TimeoutSeconds * 1000)) {
            $process.Kill()
            $process.WaitForExit()
            $combined = $stdout.Result + $stderr.Result
            [void]$combinedLog.AppendLine("=== pass $pass / $Passes ===")
            [void]$combinedLog.AppendLine($combined)
            [System.IO.File]::WriteAllText($logPath, $combinedLog.ToString())
            throw "fresh sclang device probe pass $pass timed out after ${TimeoutSeconds}s"
        }

        $combined = $stdout.Result + $stderr.Result
        [void]$combinedLog.AppendLine("=== pass $pass / $Passes ===")
        [void]$combinedLog.AppendLine($combined)
        [System.IO.File]::WriteAllText($logPath, $combinedLog.ToString())
        if ($process.ExitCode -ne 0) {
            throw "fresh sclang device probe pass $pass exited $($process.ExitCode); log: $logPath"
        }
        if ($combined -notmatch '(?m)^CQ_AUDIO_DEVICE_PROBE_OK\s*$') {
            throw "fresh sclang device probe pass $pass marker missing; log: $logPath"
        }
        if (-not (Test-Path -LiteralPath $OutputPath)) {
            throw "fresh sclang device probe pass $pass produced no output; log: $logPath"
        }
    }
} catch {
    if (Test-Path -LiteralPath $OutputPath) {
        Remove-Item -LiteralPath $OutputPath -Force
    }
    [Console]::Error.WriteLine($_.Exception.Message)
    exit 1
} finally {
    Remove-Item -LiteralPath $configPath -Force -ErrorAction SilentlyContinue
    Remove-Item Env:CQ_AUDIO_PROBE_OUTPUT -ErrorAction SilentlyContinue
}

Remove-Item -LiteralPath $logPath -Force -ErrorAction SilentlyContinue
exit 0
