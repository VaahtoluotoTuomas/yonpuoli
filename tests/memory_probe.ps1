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

# Least squares over (hours, megabytes) -> MB per hour. Reported with the
# spread, because a slope without one is a claim without an error bar.
function Get-Slope([object[]] $points) {
    $n = $points.Count
    if ($n -lt 3) { return $null }
    $sumX = 0.0; $sumY = 0.0; $sumXY = 0.0; $sumXX = 0.0
    foreach ($p in $points) {
        $sumX  += $p.H;  $sumY  += $p.Mb
        $sumXY += $p.H * $p.Mb
        $sumXX += $p.H * $p.H
    }
    $den = ($n * $sumXX) - ($sumX * $sumX)
    if ([math]::Abs($den) -lt 1e-12) { return $null }
    return (($n * $sumXY) - ($sumX * $sumY)) / $den
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
    $marker = [regex]::Matches($combined, '(?m)^CQ_MEM_PROBE_DONE\s*$').Count -gt 0
    [System.IO.File]::WriteAllText((Join-Path $outDir "$stamp-$scenario.log"), $combined)

    $slope = $null
    $first = $null
    $last  = $null
    if ($samples.Count -ge 3) {
        $slope = Get-Slope $samples
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
        Clean    = $marker
    }
    if ($null -ne $slope) {
        $row.DeltaMb  = [math]::Round($last - $first, 1)
        $row.SlopeMbH = [math]::Round($slope, 1)
    }
    $report.Add($row)

    Write-Output ("  {0,-14} {1,3} samples  {2,7} -> {3,7} MB   delta {4,6} MB   slope {5,7} MB/h  {6}" -f `
        $row.Scenario, $row.Samples, $row.StartMb, $row.EndMb, $row.DeltaMb, $row.SlopeMbH,
        $(if ($marker) { 'ok' } else { 'NO MARKER' }))
}

Write-Output ''
Write-Output 'CQ memory probe summary (measured window excludes the warm-up)'
$report | Format-Table Scenario, Samples, StartMb, EndMb, DeltaMb, SlopeMbH, Clean -AutoSize | Out-String |
    ForEach-Object { Write-Output $_ }
Write-Output "  raw samples: $outDir"
Write-Output ''
Write-Output 'CQ_MEM_PROBE_REPORT'
