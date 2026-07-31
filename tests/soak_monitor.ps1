# soak_monitor.ps1 -- unattended hardware/resource soak telemetry.
#
# ASCII ONLY. Windows PowerShell 5.1 reads a .ps1 without a BOM as ANSI, so a
# UTF-8 em dash (E2 80 94) decodes as CP1252 "a<euro>" where 0x94 is a smart
# closing quote -- which terminates any string it lands in and breaks the parse
# far below. Do not paste typographic punctuation into this file.
#
# Samples the running scsynth over OSC and both SC processes over the OS, then
# appends one CSV row per sample. Nothing here touches the instrument: the only
# packet sent is /status, which sclang's own status watcher already sends
# continuously. It never enumerates audio devices -- a running Windows WASAPI
# server must never receive _ListAudioDevices (the 2026-07-29 hang).
#
# Usage (run in its own PowerShell window so it outlives any editor session):
#   powershell -NoProfile -ExecutionPolicy Bypass -File tests/soak_monitor.ps1
#   powershell -NoProfile -ExecutionPolicy Bypass -File tests/soak_monitor.ps1 -Hours 10 -IntervalSeconds 20
#
# Print the summary of a finished (or interrupted) run without measuring:
#   powershell -NoProfile -ExecutionPolicy Bypass -File tests/soak_monitor.ps1 -SummarizeCsv tests/tmp/soak/soak-YYYYMMDD-HHMMSS.csv
#
# Stop with Ctrl+C; the CSV is flushed every sample, so an interrupted run keeps
# everything it measured, and a summary is rewritten beside it every 15 samples.
# Output lands in tests/tmp/soak/ (gitignored).

[CmdletBinding()]
param(
    [double] $Hours = 10,
    [int]    $IntervalSeconds = 20,
    [int]    $Port = 57110,
    [string] $Host_ = '127.0.0.1',
    # Requested rate the clock is judged against. The guard in Cocoquantus uses
    # the same 0.8..1.2 ratio bounds; keep them identical on purpose.
    [double] $ExpectedSampleRate = 48000,
    [double] $ClockRatioMin = 0.8,
    [double] $ClockRatioMax = 1.2,
    # Print the summary of an existing run and exit. Measures nothing, samples
    # nothing, holds nothing awake -- it only reads the evidence.
    [string] $SummarizeCsv = ''
)

$ErrorActionPreference = 'Stop'

# ---------- summary ----------
#
# SOAK-1 recorded this as tooling debt on 2026-07-31: the summary lived in a
# `finally` block, and Ctrl+C during Start-Sleep skips it, so the overnight run
# lost the very block the checklist tells the user to report. The CSV survived,
# because it is flushed every sample.
#
# The fix is not a cleverer trap. **The summary is computed from the evidence,
# never from memory**: it is derived from the CSV, written to disk beside it
# every few minutes, and reproducible afterwards from any past run with
# -SummarizeCsv. However a run ends -- Ctrl+C, a killed window, a power loss --
# the last written summary is on disk and an exact one is one command away.

function Get-SoakStats([object[]] $rows) {
    $stats = @{
        Samples = 0; Failures = 0; ClockBad = 0; PidChanges = 0
        MinSr = [double]::PositiveInfinity; MaxSr = 0.0
        MaxScsynthWs = 0.0; MaxSclangWs = 0.0
        First = ''; Last = ''; ElapsedH = 0.0
    }
    $firstPid = $null
    foreach ($r in $rows) {
        $stats.Samples++
        if ($stats.First -eq '') { $stats.First = $r.timestamp }
        $stats.Last = $r.timestamp
        if ($r.elapsed_s -ne '') {
            $stats.ElapsedH = [math]::Round([double]$r.elapsed_s / 3600, 2)
        }
        if ($r.reply_ok -eq '0') { $stats.Failures++ }
        if ($r.clock_ok -eq '0') { $stats.ClockBad++ }
        if ($r.scsynth_pid -ne '') {
            if ($null -eq $firstPid) {
                $firstPid = $r.scsynth_pid
            } elseif ($r.scsynth_pid -ne $firstPid) {
                $stats.PidChanges++
                $firstPid = $r.scsynth_pid
            }
        }
        if ($r.actual_sr -ne '') {
            $sr = [double]$r.actual_sr
            if ($sr -lt $stats.MinSr) { $stats.MinSr = $sr }
            if ($sr -gt $stats.MaxSr) { $stats.MaxSr = $sr }
        }
        if ($r.scsynth_ws_mb -ne '' -and [double]$r.scsynth_ws_mb -gt $stats.MaxScsynthWs) {
            $stats.MaxScsynthWs = [double]$r.scsynth_ws_mb
        }
        if ($r.sclang_ws_mb -ne '' -and [double]$r.sclang_ws_mb -gt $stats.MaxSclangWs) {
            $stats.MaxSclangWs = [double]$r.sclang_ws_mb
        }
    }
    if ($stats.MinSr -eq [double]::PositiveInfinity) { $stats.MinSr = 0 }
    return $stats
}

function Format-SoakSummary($stats, [string] $csv, [string] $events) {
    return @(
        '',
        'CQ_SOAK_SUMMARY',
        "  csv            : $csv",
        "  events         : $events",
        "  ran            : $($stats.ElapsedH) h over $($stats.Samples) samples",
        "  window         : $($stats.First) .. $($stats.Last)",
        "  status failures: $($stats.Failures)",
        "  unhealthy clock: $($stats.ClockBad) samples",
        "  scsynth restart: $($stats.PidChanges)",
        "  actual SR range: $([math]::Round($stats.MinSr,1)) .. $([math]::Round($stats.MaxSr,1)) Hz",
        "  peak scsynth WS: $($stats.MaxScsynthWs) MB",
        "  peak sclang  WS: $($stats.MaxSclangWs) MB"
    )
}

if (-not [string]::IsNullOrWhiteSpace($SummarizeCsv)) {
    if (-not (Test-Path -LiteralPath $SummarizeCsv)) {
        throw "csv not found: $SummarizeCsv"
    }
    $rows = @(Import-Csv -LiteralPath $SummarizeCsv)
    if ($rows.Count -eq 0) { throw "csv has no samples: $SummarizeCsv" }
    $eventGuess = [System.IO.Path]::ChangeExtension($SummarizeCsv, $null).TrimEnd('.') + '-events.log'
    Format-SoakSummary (Get-SoakStats $rows) $SummarizeCsv $eventGuess |
        ForEach-Object { Write-Output $_ }
    exit 0
}

# This laptop is a Modern Standby machine: it drops into low-power idle when the
# screen goes off, no matter what `powercfg` reports for the sleep timeout. A
# 2026-07-30 unattended run proved it: the system entered Modern Standby at
# 03:45 and the suspended audio stream dragged scsynth's running sample-rate
# average down to 7.4 kHz until it resumed. Hold the system awake for exactly as
# long as this monitor runs, and release it on every exit path, so the soak needs
# no permanent change to the user's power settings.
$keepAwakeType = @'
using System;
using System.Runtime.InteropServices;
public static class CQKeepAwake {
    [DllImport("kernel32.dll", SetLastError = true)]
    public static extern uint SetThreadExecutionState(uint esFlags);
    public const uint ES_CONTINUOUS       = 0x80000000;
    public const uint ES_SYSTEM_REQUIRED  = 0x00000001;
    public const uint ES_AWAYMODE_REQUIRED= 0x00000040;
}
'@
$keepAwakeOk = $false
try {
    Add-Type -TypeDefinition $keepAwakeType -ErrorAction Stop
    $flags = [CQKeepAwake]::ES_CONTINUOUS -bor [CQKeepAwake]::ES_SYSTEM_REQUIRED `
        -bor [CQKeepAwake]::ES_AWAYMODE_REQUIRED
    if ([CQKeepAwake]::SetThreadExecutionState($flags) -ne 0) { $keepAwakeOk = $true }
} catch { }

$root    = Split-Path -Parent (Split-Path -Parent $MyInvocation.MyCommand.Path)
$outDir  = Join-Path $root 'tests\tmp\soak'
if (-not (Test-Path $outDir)) { New-Item -ItemType Directory -Force -Path $outDir | Out-Null }

$stamp     = Get-Date -Format 'yyyyMMdd-HHmmss'
$csvPath     = Join-Path $outDir "soak-$stamp.csv"
$eventPath   = Join-Path $outDir "soak-$stamp-events.log"
$summaryPath = Join-Path $outDir "soak-$stamp-summary.txt"

# ---------- OSC ----------

function New-OscMessage([string] $address) {
    $bytes = New-Object System.Collections.Generic.List[byte]
    $addr  = [System.Text.Encoding]::ASCII.GetBytes($address)
    $bytes.AddRange($addr)
    $bytes.Add(0)
    while ($bytes.Count % 4 -ne 0) { $bytes.Add(0) }
    $tag = [System.Text.Encoding]::ASCII.GetBytes(',')
    $bytes.AddRange($tag)
    $bytes.Add(0)
    while ($bytes.Count % 4 -ne 0) { $bytes.Add(0) }
    return $bytes.ToArray()
}

function Read-OscString([byte[]] $buf, [ref] $index) {
    $start = $index.Value
    $i = $start
    while ($i -lt $buf.Length -and $buf[$i] -ne 0) { $i++ }
    $text = [System.Text.Encoding]::ASCII.GetString($buf, $start, $i - $start)
    $i++
    while ($i % 4 -ne 0) { $i++ }
    $index.Value = $i
    return $text
}

# OSC is big-endian; Windows is little-endian, so every scalar is reversed.
function Read-OscScalar([byte[]] $buf, [ref] $index, [int] $size, [string] $kind) {
    $slice = New-Object byte[] $size
    [Array]::Copy($buf, $index.Value, $slice, 0, $size)
    [Array]::Reverse($slice)
    $index.Value = $index.Value + $size
    switch ($kind) {
        'i' { return [BitConverter]::ToInt32($slice, 0) }
        'f' { return [BitConverter]::ToSingle($slice, 0) }
        'd' { return [BitConverter]::ToDouble($slice, 0) }
    }
    throw "unsupported OSC type '$kind'"
}

function Get-ServerStatus([System.Net.Sockets.UdpClient] $client,
                          [System.Net.IPEndPoint] $endpoint) {
    $probe = New-OscMessage '/status'
    [void] $client.Send($probe, $probe.Length, $endpoint)
    $remote = New-Object System.Net.IPEndPoint([System.Net.IPAddress]::Any, 0)
    $buf = $client.Receive([ref] $remote)

    $i = 0
    $address = Read-OscString $buf ([ref] $i)
    if ($address -ne '/status.reply') { throw "unexpected reply '$address'" }
    $tags = Read-OscString $buf ([ref] $i)
    # Never name this $args: that is a PowerShell automatic variable.
    $oscArgs = New-Object System.Collections.Generic.List[object]
    foreach ($ch in $tags.ToCharArray()) {
        switch ($ch) {
            ',' { }
            'i' { $oscArgs.Add((Read-OscScalar $buf ([ref] $i) 4 'i')) }
            'f' { $oscArgs.Add((Read-OscScalar $buf ([ref] $i) 4 'f')) }
            'd' { $oscArgs.Add((Read-OscScalar $buf ([ref] $i) 8 'd')) }
            default { throw "unsupported OSC type '$ch' in '$tags'" }
        }
    }
    if ($oscArgs.Count -lt 9) { throw "short /status.reply ($($oscArgs.Count) args)" }
    return [pscustomobject]@{
        Bytes      = $buf.Length
        UGens      = $oscArgs[1]
        Synths     = $oscArgs[2]
        Groups     = $oscArgs[3]
        SynthDefs  = $oscArgs[4]
        AvgCPU     = $oscArgs[5]
        PeakCPU    = $oscArgs[6]
        NominalSR  = $oscArgs[7]
        ActualSR   = $oscArgs[8]
    }
}

function Get-ProcSample([string] $name) {
    $p = Get-Process -Name $name -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($null -eq $p) {
        return [pscustomobject]@{ Pid = ''; WsMb = ''; Handles = ''; CpuS = ''; Threads = '' }
    }
    $cpu = ''
    try { $cpu = [math]::Round($p.TotalProcessorTime.TotalSeconds, 1) } catch { }
    return [pscustomobject]@{
        Pid     = $p.Id
        WsMb    = [math]::Round($p.WorkingSet64 / 1MB, 1)
        Handles = $p.Handles
        CpuS    = $cpu
        Threads = $p.Threads.Count
    }
}

function Write-Event([string] $line) {
    $text = "$(Get-Date -Format 'yyyy-MM-dd HH:mm:ss')  $line"
    Add-Content -Path $eventPath -Value $text -Encoding utf8
    Write-Warning $line
}

# ---------- run ----------

$columns = @(
    'timestamp','elapsed_s','reply_ok','reply_bytes','ugens','synths','groups',
    'synthdefs','avg_cpu','peak_cpu','nominal_sr','actual_sr','sr_ratio','clock_ok',
    'scsynth_pid','scsynth_ws_mb','scsynth_handles','scsynth_cpu_s','scsynth_threads',
    'sclang_pid','sclang_ws_mb','sclang_handles','sclang_cpu_s','sclang_threads'
) -join ','
Set-Content -Path $csvPath -Value $columns -Encoding utf8

$client = New-Object System.Net.Sockets.UdpClient
$client.Client.ReceiveTimeout = 2000
$endpoint = New-Object System.Net.IPEndPoint([System.Net.IPAddress]::Parse($Host_), $Port)

$startedAt = Get-Date
$deadline  = $startedAt.AddHours($Hours)
$sample    = 0
$failures  = 0
$clockBad  = 0
$firstPid  = $null
$pidChanges = 0
$minSr = [double]::PositiveInfinity
$maxSr = 0.0
$maxScsynthWs = 0.0
$maxSclangWs  = 0.0
$lastClockOk = $true

Write-Output "soak monitor -> $csvPath"
Write-Output "sampling every $IntervalSeconds s until $($deadline.ToString('yyyy-MM-dd HH:mm:ss')) (Ctrl+C stops)"
if ($keepAwakeOk) {
    Write-Output "keep-awake held (system will not enter Modern Standby while this runs)"
} else {
    Write-Warning "keep-awake FAILED: an unattended run may sleep and produce a suspended-stream artefact"
}
Write-Output ''

try {
    while ((Get-Date) -lt $deadline) {
        $sample++
        $now     = Get-Date
        $elapsed = [math]::Round(($now - $startedAt).TotalSeconds, 0)

        $status = $null
        $err = ''
        try { $status = Get-ServerStatus $client $endpoint } catch { $err = $_.Exception.Message }

        $scsynth = Get-ProcSample 'scsynth'
        $sclang  = Get-ProcSample 'sclang'

        if ($scsynth.Pid -ne '') {
            if ($null -eq $firstPid) {
                $firstPid = $scsynth.Pid
            } elseif ($scsynth.Pid -ne $firstPid) {
                $pidChanges++
                Write-Event "scsynth PID changed $firstPid -> $($scsynth.Pid) (the server restarted or died)"
                $firstPid = $scsynth.Pid
            }
            if ($scsynth.WsMb -gt $maxScsynthWs) { $maxScsynthWs = $scsynth.WsMb }
        }
        if ($sclang.WsMb -ne '' -and $sclang.WsMb -gt $maxSclangWs) { $maxSclangWs = $sclang.WsMb }

        if ($null -eq $status) {
            $failures++
            Write-Event "no /status.reply: $err"
            $row = @(
                $now.ToString('yyyy-MM-dd HH:mm:ss'), $elapsed, 0, '', '', '', '', '',
                '', '', '', '', '', '',
                $scsynth.Pid, $scsynth.WsMb, $scsynth.Handles, $scsynth.CpuS, $scsynth.Threads,
                $sclang.Pid, $sclang.WsMb, $sclang.Handles, $sclang.CpuS, $sclang.Threads
            ) -join ','
        } else {
            $ratio = 0.0
            if ($ExpectedSampleRate -gt 0) {
                $ratio = [math]::Round($status.ActualSR / $ExpectedSampleRate, 4)
            }
            $clockOk = ($ratio -ge $ClockRatioMin) -and ($ratio -le $ClockRatioMax)
            if (-not $clockOk) {
                $clockBad++
                if ($lastClockOk) {
                    Write-Event ("audio clock unhealthy: actual {0} Hz vs expected {1} Hz (ratio {2})" -f `
                        [math]::Round($status.ActualSR, 1), $ExpectedSampleRate, $ratio)
                }
            } elseif (-not $lastClockOk) {
                Write-Event ("audio clock recovered: actual {0} Hz" -f [math]::Round($status.ActualSR, 1))
            }
            $lastClockOk = $clockOk

            if ($status.ActualSR -lt $minSr) { $minSr = $status.ActualSR }
            if ($status.ActualSR -gt $maxSr) { $maxSr = $status.ActualSR }

            $okFlag = 0
            if ($clockOk) { $okFlag = 1 }
            $row = @(
                $now.ToString('yyyy-MM-dd HH:mm:ss'), $elapsed, 1, $status.Bytes,
                $status.UGens, $status.Synths, $status.Groups, $status.SynthDefs,
                [math]::Round($status.AvgCPU, 2), [math]::Round($status.PeakCPU, 2),
                [math]::Round($status.NominalSR, 1), [math]::Round($status.ActualSR, 1),
                $ratio, $okFlag,
                $scsynth.Pid, $scsynth.WsMb, $scsynth.Handles, $scsynth.CpuS, $scsynth.Threads,
                $sclang.Pid, $sclang.WsMb, $sclang.Handles, $sclang.CpuS, $sclang.Threads
            ) -join ','

            if (($sample -eq 1) -or ($sample % 15 -eq 0)) {
                Write-Output ("{0}  t+{1,6}s  sr {2,8}  cpu {3,5}/{4,5}  synths {5,3}  scsynth {6,6} MB / {7,5} h  sclang {8,7} MB / {9,5} h" -f `
                    $now.ToString('HH:mm:ss'), $elapsed, [math]::Round($status.ActualSR, 1),
                    [math]::Round($status.AvgCPU, 1), [math]::Round($status.PeakCPU, 1),
                    $status.Synths, $scsynth.WsMb, $scsynth.Handles, $sclang.WsMb, $sclang.Handles)
            }
        }

        Add-Content -Path $csvPath -Value $row -Encoding utf8
        # Keep a summary on disk from the run's own evidence, so an interrupted
        # window still leaves one behind (the 2026-07-31 debt).
        if (($sample % 15) -eq 0) {
            try {
                $sofar = @(Import-Csv -LiteralPath $csvPath)
                if ($sofar.Count -gt 0) {
                    Set-Content -Path $summaryPath -Encoding utf8 -Value (
                        Format-SoakSummary (Get-SoakStats $sofar) $csvPath $eventPath)
                }
            } catch { }
        }
        Start-Sleep -Seconds $IntervalSeconds
    }
} finally {
    $client.Close()
    if ($keepAwakeOk) {
        [void] [CQKeepAwake]::SetThreadExecutionState([CQKeepAwake]::ES_CONTINUOUS)
    }
    # Derived from the CSV, not from the counters this loop happens to hold:
    # one definition of the summary, and it is the one the evidence supports.
    $summary = @('', 'CQ_SOAK_SUMMARY  (no samples were written)')
    if (Test-Path -LiteralPath $csvPath) {
        $rows = @(Import-Csv -LiteralPath $csvPath)
        if ($rows.Count -gt 0) {
            $summary = Format-SoakSummary (Get-SoakStats $rows) $csvPath $eventPath
        }
    }
    $summary | ForEach-Object { Write-Output $_ }
    Set-Content -Path $summaryPath -Value $summary -Encoding utf8
    Add-Content -Path $eventPath -Value ($summary -join [Environment]::NewLine) -Encoding utf8
}
