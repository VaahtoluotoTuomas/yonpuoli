param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$Script,

    [int]$TimeoutSeconds = 0,

    [string]$Sclang = "C:\Program Files\SuperCollider-3.13.0\sclang.exe"
)

$ErrorActionPreference = "Stop"
$repoRoot = Split-Path -Parent $PSScriptRoot
$scriptPath = Join-Path $repoRoot $Script
$confPath = Join-Path $PSScriptRoot "sclang_conf_dev.yaml"

if (-not (Test-Path -LiteralPath $Sclang)) {
    throw "sclang not found: $Sclang"
}
if (-not (Test-Path -LiteralPath $scriptPath)) {
    throw "gate script not found: $scriptPath"
}

if ($TimeoutSeconds -le 0) {
    $leaf = Split-Path -Leaf $Script
    $TimeoutSeconds = switch -Wildcard ($leaf) {
        "compile_check.scd" { 15; break }
        "run_all.scd" { 60; break }
        "gui_night_smoke.scd" { 105; break }
        "nrt_*.scd" { 180; break }
        default { 60 }
    }
}

$startInfo = [System.Diagnostics.ProcessStartInfo]::new()
$startInfo.FileName = $Sclang
$startInfo.Arguments = "-l `"$confPath`" -i none `"$scriptPath`""
$startInfo.WorkingDirectory = $repoRoot
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
    [Console]::Out.Write($stdout.Result)
    [Console]::Error.Write($stderr.Result)
    [Console]::Error.WriteLine(
        "CQ_GATE_WATCHDOG_TIMEOUT: $Script exceeded ${TimeoutSeconds}s (PID $($process.Id) terminated)."
    )
    exit 124
}

[Console]::Out.Write($stdout.Result)
[Console]::Error.Write($stderr.Result)
exit $process.ExitCode
