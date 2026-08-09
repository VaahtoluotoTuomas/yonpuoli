# run_soak_validation.ps1 -- gated production-path validation of SOAK-1 memory.
# ASCII ONLY. This wrapper never launches sclang directly.

[CmdletBinding()]
param(
    [double] $Minutes = 95,
    [int]    $IntervalSeconds = 20,
    [int]    $MemoryWarmupSeconds = 3600,
    [int]    $MemoryWindowSeconds = 900,
    [double] $PrivateSlopeMinPostWarmupHours = 2.0,
    [double] $LiveHeapGrowthPercent = 5.0,
    [int]    $LiveHeapGrowthFloorCells = 5000,
    [switch] $AllowInsufficientMemory,
    # Empty = resolve from SC_LANG, tests/dev_paths.scd, or the newest install.
    [string] $Sclang = ''
)

$ErrorActionPreference = 'Stop'
if ([string]::IsNullOrWhiteSpace($Sclang)) {
    $Sclang = & (Join-Path $PSScriptRoot 'resolve_sclang.ps1')
}
$repoRoot = Split-Path -Parent $PSScriptRoot
$gate = Join-Path $PSScriptRoot 'run_sclang_gate.ps1'
$monitor = Join-Path $PSScriptRoot 'soak_monitor.ps1'
$powershell = Join-Path $PSHOME 'powershell.exe'
$tmpDir = Join-Path $PSScriptRoot 'tmp'
$request = Join-Path $tmpDir 'soak_validation_request.txt'
$ready = Join-Path $tmpDir 'soak_validation.ready'
$stop = Join-Path $tmpDir 'soak_validation.stop'
$pidFile = Join-Path $tmpDir 'soak_validation-sclang.pid'

if ($Minutes -le 0) { throw 'Minutes must be positive' }
$monitorSeconds = [int][math]::Ceiling($Minutes * 60.0)
if ($monitorSeconds -lt ($MemoryWarmupSeconds + (2 * $MemoryWindowSeconds))) {
    throw 'duration must include warmup plus two complete memory windows'
}
foreach ($path in @($gate, $monitor, $powershell, $Sclang)) {
    if (-not (Test-Path -LiteralPath $path)) { throw "not found: $path" }
}
$existing = @(Get-Process -Name sclang,scsynth -ErrorAction SilentlyContinue)
if ($existing.Count -gt 0) {
    throw 'validation requires no other sclang or scsynth process'
}

[System.IO.Directory]::CreateDirectory($tmpDir) | Out-Null
foreach ($path in @($ready, $stop, $pidFile)) {
    if (Test-Path -LiteralPath $path) { [System.IO.File]::Delete($path) }
}
$scriptSeconds = $monitorSeconds + 180
[System.IO.File]::WriteAllText(
    $request,
    "$scriptSeconds $MemoryWarmupSeconds",
    [System.Text.Encoding]::ASCII)

function New-CapturedProcess([string] $arguments) {
    $info = [System.Diagnostics.ProcessStartInfo]::new()
    $info.FileName = $powershell
    $info.Arguments = $arguments
    $info.WorkingDirectory = $repoRoot
    $info.UseShellExecute = $false
    $info.CreateNoWindow = $true
    $info.RedirectStandardOutput = $true
    $info.RedirectStandardError = $true
    $process = [System.Diagnostics.Process]::new()
    $process.StartInfo = $info
    [void]$process.Start()
    return [pscustomobject]@{
        Process = $process
        Stdout = $process.StandardOutput.ReadToEndAsync()
        Stderr = $process.StandardError.ReadToEndAsync()
    }
}

$gateRun = $null
$monitorRun = $null
$failure = $null
$startedAt = Get-Date
try {
    $gateRun = New-CapturedProcess (
        "-NoProfile -ExecutionPolicy Bypass -File `"$gate`" " +
        "`"tests/soak_validation.scd`" -TimeoutSeconds $($scriptSeconds + 60) " +
        "-Sclang `"$Sclang`" -PidFile `"$pidFile`"")

    $readyDeadline = (Get-Date).AddSeconds(120)
    while ((-not (Test-Path -LiteralPath $ready)) -and
           (-not $gateRun.Process.HasExited) -and ((Get-Date) -lt $readyDeadline)) {
        Start-Sleep -Milliseconds 200
    }
    if (-not (Test-Path -LiteralPath $ready)) {
        throw 'soak validation did not publish its ready marker'
    }

    $hours = ($monitorSeconds / 3600.0).ToString(
        [System.Globalization.CultureInfo]::InvariantCulture)
    $monitorRun = New-CapturedProcess (
        "-NoProfile -ExecutionPolicy Bypass -File `"$monitor`" " +
        "-Hours $hours -IntervalSeconds $IntervalSeconds " +
        "-MemoryWarmupSeconds $MemoryWarmupSeconds " +
        "-MemoryWindowSeconds $MemoryWindowSeconds " +
        "-PrivateSlopeMinPostWarmupHours $PrivateSlopeMinPostWarmupHours")
    $monitorRun.Process.WaitForExit()
    [System.IO.File]::WriteAllText($stop, 'stop', [System.Text.Encoding]::ASCII)

    if (-not $gateRun.Process.WaitForExit(120000)) {
        throw 'gate did not finish within 120 seconds of the monitor'
    }

    $monitorOutput = $monitorRun.Stdout.Result + $monitorRun.Stderr.Result
    $gateOutput = $gateRun.Stdout.Result + $gateRun.Stderr.Result
    [Console]::Out.Write($monitorOutput)
    [Console]::Out.Write($gateOutput)
    if ($monitorRun.Process.ExitCode -ne 0) {
        throw "soak monitor exited $($monitorRun.Process.ExitCode)"
    }
    if ($gateRun.Process.ExitCode -ne 0) {
        throw "sclang gate exited $($gateRun.Process.ExitCode)"
    }

    $gcMatches = [regex]::Matches(
        $gateOutput,
        '(?m)^tot bwf t sz:\s+(\d+)\s+(\d+)\s+(\d+)\s+(\d+)\s+(\d+)\s*$')
    if ($gcMatches.Count -lt 2) {
        throw 'validation log did not contain start and end GC totals'
    }
    $gcStart = $gcMatches[0]
    $gcEnd = $gcMatches[$gcMatches.Count - 1]
    $startLive = [int64]$gcStart.Groups[1].Value + [int64]$gcStart.Groups[2].Value
    $endLive = [int64]$gcEnd.Groups[1].Value + [int64]$gcEnd.Groups[2].Value
    $liveAllowance = [math]::Max(
        $startLive * ($LiveHeapGrowthPercent / 100.0),
        $LiveHeapGrowthFloorCells)
    $heapPassed = ($endLive - $startLive) -le $liveAllowance
    if ((-not $heapPassed) -and (-not $AllowInsufficientMemory)) {
        throw "SC live heap grew $($endLive - $startLive) cells (allowance $liveAllowance)"
    }
    $heapStatus = if ($heapPassed) { 'pass' } else { 'informational' }
    Write-Output (
        "CQ_SOAK_HEAP_CRITERION $heapStatus live $startLive -> $endLive cells")

    $bookMatches = [regex]::Matches(
        $gateOutput,
        '(?m)^CQ_SOAK_MEMORY .* spellbookEntries (\d+) spellbookLines (\d+)\s*$')
    if ($bookMatches.Count -lt 2) {
        throw 'validation log did not contain start and end Spellbook totals'
    }
    $endBookLines = [int]$bookMatches[$bookMatches.Count - 1].Groups[2].Value
    if ($endBookLines -gt 20000) {
        throw "Spellbook exceeded 20000 lines ($endBookLines)"
    }
    Write-Output "CQ_SOAK_SPELLBOOK_CRITERION pass ($endBookLines lines)"

    $memoryPassed = $monitorOutput -match '(?m)^  memory criterion: pass\s*$'
    $memoryInsufficient = $monitorOutput -match
        '(?m)^  memory criterion: insufficient\s*$'
    if ((-not $memoryPassed) -and
        (-not ($AllowInsufficientMemory -and $memoryInsufficient))) {
        throw 'replacement memory criterion did not pass'
    }
    'CQ_SOAK_VALIDATION_REPORT'
} catch {
    $failure = $_
    throw
} finally {
    if (Test-Path -LiteralPath $ready) { [System.IO.File]::Delete($ready) }
    if (Test-Path -LiteralPath $stop) { [System.IO.File]::Delete($stop) }
    if ($null -ne $monitorRun -and -not $monitorRun.Process.HasExited) {
        $monitorRun.Process.Kill()
        $monitorRun.Process.WaitForExit()
    }
    if ($null -ne $gateRun -and -not $gateRun.Process.HasExited) {
        [System.IO.File]::WriteAllText($stop, 'stop', [System.Text.Encoding]::ASCII)
        if (-not $gateRun.Process.WaitForExit(30000)) {
            if (Test-Path -LiteralPath $pidFile) {
                $childPid = [int][System.IO.File]::ReadAllText($pidFile)
                $child = Get-Process -Id $childPid -ErrorAction SilentlyContinue
                if ($null -ne $child) { $child.Kill(); $child.WaitForExit() }
            }
        }
    }
    $leftoverServer = @(Get-Process -Name scsynth -ErrorAction SilentlyContinue |
        Where-Object { $_.StartTime -ge $startedAt })
    foreach ($serverProcess in $leftoverServer) {
        $serverProcess.Kill()
        $serverProcess.WaitForExit()
    }
    if ($null -ne $gateRun -and -not $gateRun.Process.HasExited) {
        if (-not $gateRun.Process.WaitForExit(10000)) {
            $gateRun.Process.Kill()
            $gateRun.Process.WaitForExit()
        }
    }
    if ($null -ne $failure) {
        if ($null -ne $monitorRun) {
            [Console]::Out.Write($monitorRun.Stdout.Result + $monitorRun.Stderr.Result)
        }
        if ($null -ne $gateRun) {
            [Console]::Out.Write($gateRun.Stdout.Result + $gateRun.Stderr.Result)
        }
    }
    if (Test-Path -LiteralPath $stop) { [System.IO.File]::Delete($stop) }
}
