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
# Stop with Ctrl+C; the CSV is flushed every sample, so an interrupted run keeps
# everything it measured. Output lands in tests/tmp/soak/ (gitignored).

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
    [double] $ClockRatioMax = 1.2
)

$ErrorActionPreference = 'Stop'

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
$csvPath   = Join-Path $outDir "soak-$stamp.csv"
$eventPath = Join-Path $outDir "soak-$stamp-events.log"

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
        Start-Sleep -Seconds $IntervalSeconds
    }
} finally {
    $client.Close()
    if ($keepAwakeOk) {
        [void] [CQKeepAwake]::SetThreadExecutionState([CQKeepAwake]::ES_CONTINUOUS)
    }
    $ranFor = [math]::Round(((Get-Date) - $startedAt).TotalHours, 2)
    if ($minSr -eq [double]::PositiveInfinity) { $minSr = 0 }
    $minSr = [math]::Round($minSr, 1)
    $maxSr = [math]::Round($maxSr, 1)
    $summary = @(
        '',
        "CQ_SOAK_SUMMARY",
        "  csv            : $csvPath",
        "  events         : $eventPath",
        "  ran            : $ranFor h over $sample samples",
        "  status failures: $failures",
        "  unhealthy clock: $clockBad samples",
        "  scsynth restart: $pidChanges",
        "  actual SR range: $minSr .. $maxSr Hz",
        "  peak scsynth WS: $maxScsynthWs MB",
        "  peak sclang  WS: $maxSclangWs MB"
    )
    $summary | ForEach-Object { Write-Output $_ }
    Add-Content -Path $eventPath -Value ($summary -join [Environment]::NewLine) -Encoding utf8
}
