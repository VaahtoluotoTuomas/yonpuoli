# memory_probe.ps1 -- SOAK-1 criterion 5: WHERE does sclang's working set grow?
#
# ASCII ONLY (see soak_monitor.ps1's header for why a typographic dash breaks
# this file's parse hundreds of lines below where it sits).
#
# The soak measured ~20 MB/h of growth without settling, twice, on both the old
# and the fixed code. That run answers "does it plateau" and costs a night. This
# one answers "which workload allocates" and costs minutes: it launches one
# sclang per scenario, samples that process's working set from outside, and
# fits a slope. No server and no audio device are involved -- the growth is on
# the language side, and every scenario builds the same serverless GUI the
# smokes do.
#
# Usage (run with no soak monitor and no other sclang alive):
#   powershell -NoProfile -ExecutionPolicy Bypass -File tests/memory_probe.ps1
#   powershell -NoProfile -ExecutionPolicy Bypass -File tests/memory_probe.ps1 -Seconds 300 -Scenarios idleGui,telemetryGui
#
# The six default scenarios answer "which workload allocates". The 2026-07-31 run
# answered it: only morphGui grows. The second stage takes that one workload
# apart and is NOT in the default list, because it is a different question:
#   -Scenarios morphGui,morphNoGui,readNoGui,commitGui
# It found CQSpellbook's uncapped journal. Verifying the cap needs a THIRD call,
# because the cap is only reached after ~57 dense acts and no 9 s scenario gets
# there inside one window -- commitGuiFast does, during its warm-up:
#   -Scenarios commitGuiFast
#
# Prints one row per scenario and CQ_MEM_PROBE_REPORT as its last line.

[CmdletBinding()]
param(
    [int]      $Seconds = 480,
    [string[]] $Scenarios = @('idleGui', 'telemetryGui', 'matrixGui', 'matrixNoGui', 'paramGui', 'morphGui'),
    [int]      $IntervalSeconds = 5,
    # Samples inside this many seconds of the start are ignored: a fresh
    # interpreter allocates its class library and first views, and that ramp is
    # not the one under investigation.
    [int]      $WarmupSeconds = 90,
    [string]   $Sclang = 'C:\Program Files\SuperCollider-3.13.0\sclang.exe'
)

$ErrorActionPreference = 'Stop'

# `powershell -File probe.ps1 -Scenarios a,b,c` hands this parameter ONE string:
# -File does not parse its arguments as PowerShell, so the comma list is not an
# array. Measured 2026-07-31 -- the whole list arrived as a single scenario name
# and the run reported a clean, flat, entirely empty row. Split here so both call
# styles mean the same thing.
$Scenarios = @($Scenarios |
    ForEach-Object { $_ -split ',' } |
    ForEach-Object { $_.Trim() } |
    Where-Object { $_ -ne '' })
if ($Scenarios.Count -lt 1) { throw 'no scenarios given' }

$repoRoot = Split-Path -Parent $PSScriptRoot
$script   = Join-Path $repoRoot 'tests\memory_probe.scd'
$conf     = Join-Path $PSScriptRoot 'sclang_conf_dev.yaml'
$request  = Join-Path $PSScriptRoot 'tmp\mem_probe.txt'
$outDir   = Join-Path $PSScriptRoot 'tmp\memprobe'

foreach ($p in @($Sclang, $script, $conf)) {
    if (-not (Test-Path -LiteralPath $p)) { throw "not found: $p" }
}
[System.IO.Directory]::CreateDirectory($outDir) | Out-Null
[System.IO.Directory]::CreateDirectory((Split-Path -Parent $request)) | Out-Null

if ($Seconds -le ($WarmupSeconds + (4 * $IntervalSeconds))) {
    throw "Seconds ($Seconds) leaves no measured window after the $WarmupSeconds s warm-up"
}

# Theil-Sen over (hours, megabytes) -> MB per hour: the median of every pairwise
# slope.
#
# Least squares stood here first and it lied. On the 2026-07-31 run it reported
# -939.6 MB/h for idleGui, whose working set was flat within 1.8 MB for 400 s and
# then fell 108 MB in a single sample when Windows trimmed it. One step-down
# drags a least-squares fit into a ramp that never happened, and telling a ramp
# from no ramp is the only thing this probe is for. The median pair is not moved
# by a step most pairs do not span, and the steps are counted separately below so
# they stay visible instead of being averaged into the answer.
function Get-TheilSen([object[]] $points) {
    $n = $points.Count
    if ($n -lt 3) { return $null }
    $slopes = New-Object System.Collections.Generic.List[double]
    for ($i = 0; $i -lt ($n - 1); $i++) {
        for ($j = $i + 1; $j -lt $n; $j++) {
            $dh = $points[$j].H - $points[$i].H
            if ([math]::Abs($dh) -gt 1e-9) {
                $slopes.Add(($points[$j].Mb - $points[$i].Mb) / $dh)
            }
        }
    }
    if ($slopes.Count -lt 1) { return $null }
    $sorted = $slopes.ToArray()
    [array]::Sort($sorted)
    $m = $sorted.Count
    if (($m % 2) -eq 1) { return $sorted[[int](($m - 1) / 2)] }
    return ($sorted[[int]($m / 2) - 1] + $sorted[[int]($m / 2)]) / 2
}

# Sample-to-sample falls of at least $minMb. Windows trims a working set in one
# step; an allocating language heap does not fall at all. Counting them keeps a
# trim readable as a trim.
function Get-Drops([object[]] $points, [double] $minMb) {
    $count = 0
    $total = 0.0
    for ($i = 1; $i -lt $points.Count; $i++) {
        $fall = $points[$i - 1].Mb - $points[$i].Mb
        if ($fall -ge $minMb) { $count++; $total += $fall }
    }
    return [pscustomobject]@{ Count = $count; TotalMb = [math]::Round($total, 1) }
}

$stamp  = Get-Date -Format 'yyyyMMdd-HHmmss'
$report = New-Object System.Collections.Generic.List[object]

Write-Output "CQ memory probe -- $($Scenarios.Count) scenarios x $Seconds s"
Write-Output "sampling every $IntervalSeconds s, ignoring the first $WarmupSeconds s"
Write-Output ''

foreach ($scenario in $Scenarios) {
    Set-Content -Path $request -Value "$scenario $Seconds" -Encoding ascii

    $startInfo = [System.Diagnostics.ProcessStartInfo]::new()
    $startInfo.FileName  = $Sclang
    $startInfo.Arguments = "-D -l `"$conf`" -i none `"$script`""
    $startInfo.WorkingDirectory = $repoRoot
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow  = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError  = $true

    $proc = [System.Diagnostics.Process]::new()
    $proc.StartInfo = $startInfo
    [void]$proc.Start()
    $stdout = $proc.StandardOutput.ReadToEndAsync()
    $stderr = $proc.StandardError.ReadToEndAsync()

    $csv = Join-Path $outDir "$stamp-$scenario.csv"
    Set-Content -Path $csv -Value 'elapsed_s,ws_mb,handles,threads' -Encoding ascii

    $began   = Get-Date
    $samples = New-Object System.Collections.Generic.List[object]
    $deadline = $began.AddSeconds($Seconds + 30)

    while ((-not $proc.HasExited) -and ((Get-Date) -lt $deadline)) {
        Start-Sleep -Seconds $IntervalSeconds
        if ($proc.HasExited) { break }
        try { $proc.Refresh() } catch { break }
        $elapsed = ((Get-Date) - $began).TotalSeconds
        $mb = [math]::Round($proc.WorkingSet64 / 1MB, 2)
        Add-Content -Path $csv -Encoding ascii -Value (
            "$([math]::Round($elapsed,0)),$mb,$($proc.Handles),$($proc.Threads.Count)")
        if ($elapsed -ge $WarmupSeconds) {
            $samples.Add([pscustomobject]@{ H = $elapsed / 3600; Mb = $mb })
        }
    }

    if (-not $proc.HasExited) {
        try { $proc.Kill(); $proc.WaitForExit() } catch { }
    }
    $combined = $stdout.Result + $stderr.Result
    # Two ways a row can be worthless: the workload never finished, or it threw.
    # An sclang error inside a scenario's Routine kills that Routine and leaves a
    # perfectly flat, perfectly meaningless curve behind, so it cannot count as
    # clean either.
    $marker  = [regex]::Matches($combined, '(?m)^CQ_MEM_PROBE_DONE\s*$').Count -gt 0
    $errored = [bool]([regex]::IsMatch($combined, 'ERROR:'))
    $marker  = $marker -and (-not $errored)
    [System.IO.File]::WriteAllText((Join-Path $outDir "$stamp-$scenario.log"), $combined)

    $slope = $null
    $first = $null
    $last  = $null
    $drops = $null
    if ($samples.Count -ge 3) {
        $slope = Get-TheilSen $samples
        $drops = Get-Drops $samples 5.0
        $first = $samples[0].Mb
        $last  = $samples[$samples.Count - 1].Mb
    }

    $row = [pscustomobject]@{
        Scenario = $scenario
        Samples  = $samples.Count
        StartMb  = $first
        EndMb    = $last
        DeltaMb  = $null
        SlopeMbH = $null
        Drops    = $(if ($null -ne $drops) { $drops.Count } else { $null })
        DropMb   = $(if ($null -ne $drops) { $drops.TotalMb } else { $null })
        Clean    = $marker
    }
    if ($null -ne $slope) {
        $row.DeltaMb  = [math]::Round($last - $first, 1)
        $row.SlopeMbH = [math]::Round($slope, 1)
    }
    $report.Add($row)

    Write-Output ("  {0,-14} {1,3} samples  {2,7} -> {3,7} MB   delta {4,6} MB   slope {5,7} MB/h   drops {6} ({7} MB)  {8}" -f `
        $row.Scenario, $row.Samples, $row.StartMb, $row.EndMb, $row.DeltaMb, $row.SlopeMbH,
        $row.Drops, $row.DropMb,
        $(if ($marker) { 'ok' } elseif ($errored) { 'ERRORS IN LOG' } else { 'NO MARKER' }))
}

Write-Output ''
Write-Output 'CQ memory probe summary (measured window excludes the warm-up)'
$report | Format-Table Scenario, Samples, StartMb, EndMb, DeltaMb, SlopeMbH, Drops, DropMb, Clean -AutoSize | Out-String |
    ForEach-Object { Write-Output $_ }
Write-Output "  raw samples: $outDir"
Write-Output ''
Write-Output 'CQ_MEM_PROBE_REPORT'
