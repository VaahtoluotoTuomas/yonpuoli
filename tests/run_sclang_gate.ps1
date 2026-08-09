param(
    [Parameter(Mandatory = $true, Position = 0)]
    [string]$Script,

    [int]$TimeoutSeconds = 0,

    # Empty = resolve from SC_LANG, tests/dev_paths.scd, or the newest install.
    # No machine's path is hardcoded here (see tests/resolve_sclang.ps1).
    [string]$Sclang = "",

    [string]$Conf = "",

    # Optional exact child-PID handoff for external resource samplers. The gate
    # still owns and validates this process; consumers must never launch sclang
    # themselves merely to observe its working set.
    [string]$PidFile = ""
)

$ErrorActionPreference = "Stop"

function Stop-GatedProcessTree {
    param([System.Diagnostics.Process]$OwnedProcess)

    if ($null -eq $OwnedProcess -or $OwnedProcess.HasExited) { return }
    $taskkill = Join-Path $env:SystemRoot "System32\taskkill.exe"
    try {
        & $taskkill /PID $OwnedProcess.Id /T /F 2>$null | Out-Null
        $OwnedProcess.WaitForExit()
    } catch {
        if (-not $OwnedProcess.HasExited) {
            $OwnedProcess.Kill()
            $OwnedProcess.WaitForExit()
        }
    }
}

if ([string]::IsNullOrWhiteSpace($Sclang)) {
    $Sclang = & (Join-Path $PSScriptRoot 'resolve_sclang.ps1')
}
$repoRoot = Split-Path -Parent $PSScriptRoot
$scriptPath = Join-Path $repoRoot $Script
$confPath = if ([string]::IsNullOrWhiteSpace($Conf)) {
    Join-Path $PSScriptRoot "sclang_conf_dev.yaml"
} elseif ([System.IO.Path]::IsPathRooted($Conf)) {
    $Conf
} else {
    Join-Path $repoRoot $Conf
}
$leaf = Split-Path -Leaf $Script
$requiredMarker = switch -Wildcard ($leaf) {
    "compile_check.scd" { "CQ_COMPILE_OK"; break }
    "run_all.scd" { "CQ_TESTS_PASSED"; break }
    "nrt_safety.scd" { "CQ_SAFETY_PASSED"; break }
    "nrt_hold_integrity.scd" { "CQ_HOLD_OK"; break }
    "nrt_dust_character.scd" { "CQ_DUST_OK"; break }
    "nrt_selfnoise_character.scd" { "CQ_SELFNOISE_OK"; break }
    "nrt_zipper_measure.scd" { "CQ_ZIPPER_MEASURED"; break }
    "nrt_preamp_loudness.scd" { "CQ_PREAMP_OK"; break }
    "nrt_playability.scd" { "CQ_PLAYABILITY_MEASURED"; break }
    "nrt_matrix_inputs.scd" { "CQ_MATRIX_INPUTS_OK"; break }
    "nrt_shake_ecology.scd" { "CQ_SHAKE_ECOLOGY_OK"; break }
    "nrt_release_content.scd" { "CQ_RELEASE_CONTENT_OK"; break }
    "generate_release_snapshots.scd" { "CQ_RELEASE_SNAPSHOTS_WRITTEN"; break }
    "morph_profile.scd" { "CQ_MORPH_PROFILE_DONE"; break }
    "morph_profile_gui.scd" { "CQ_MORPH_GUI_PROFILE_DONE"; break }
    "memory_probe.scd" { "CQ_MEM_PROBE_DONE"; break }
    "soak_validation.scd" { "CQ_SOAK_VALIDATION_DONE"; break }
    "prototype_space_compile.scd" { "CQ_SPACE_LAB_COMPILE_OK"; break }
    "prototype_space_nrt.scd" { "CQ_SPACE_LAB_NRT_PASSED"; break }
    "gui_shell_smoke.scd" { "CQ_GUI_SHELL_PASSED"; break }
    "gui_drawer_smoke.scd" { "CQ_GUI_DRAWER_PASSED"; break }
    "gui_audio_setup_smoke.scd" { "CQ_GUI_AUDIO_PASSED"; break }
    "gui_audio_presence_smoke.scd" { "CQ_GUI_AUDIO_PRESENCE_PASSED"; break }
    "gui_drawer_preview_reentry_smoke.scd" { "CQ_GUI_DRAWER_REENTRY_PASSED"; break }
    "gui_night_smoke.scd" { "CQ_GUI_NIGHT_PASSED"; break }
    "gui_night_phase_layer_smoke.scd" { "CQ_GUI_PHASE_LAYER_PASSED"; break }
    "gui_night_capture.scd" { "CQ_NIGHT_CAPTURE_DONE"; break }
    "gui_release_capture.scd" { "CQ_RELEASE_CAPTURE_DONE"; break }
    "launcher_app_smoke.scd" { "CQ_YONPUOLI_APP_SCRIPT_OK"; break }
    default { $null }
}
$logDir = Join-Path $PSScriptRoot "tmp\gate-logs"
[System.IO.Directory]::CreateDirectory($logDir) | Out-Null
$stamp = Get-Date -Format "yyyyMMdd-HHmmss-fff"
$safeLeaf = [System.IO.Path]::GetFileNameWithoutExtension($leaf)
$logPath = Join-Path $logDir "$stamp-$safeLeaf.log"

if (-not (Test-Path -LiteralPath $Sclang)) {
    throw "sclang not found: $Sclang"
}
if (-not (Test-Path -LiteralPath $scriptPath)) {
    throw "gate script not found: $scriptPath"
}
if (-not (Test-Path -LiteralPath $confPath)) {
    throw "sclang config not found: $confPath"
}

if ($TimeoutSeconds -le 0) {
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
# -D is essential: if class-library compilation fails before the .scd file can
# call exit, sclang must not fall back to an interactive prompt.
$startInfo.Arguments = "-D -l `"$confPath`" -i none `"$scriptPath`""
$startInfo.WorkingDirectory = $repoRoot
$startInfo.UseShellExecute = $false
$startInfo.CreateNoWindow = $true
$startInfo.RedirectStandardOutput = $true
$startInfo.RedirectStandardError = $true

$process = [System.Diagnostics.Process]::new()
$process.StartInfo = $startInfo
[void]$process.Start()
if (-not [string]::IsNullOrWhiteSpace($PidFile)) {
    $resolvedPidFile = if ([System.IO.Path]::IsPathRooted($PidFile)) {
        $PidFile
    } else {
        Join-Path $repoRoot $PidFile
    }
    $pidDir = Split-Path -Parent $resolvedPidFile
    if (-not [string]::IsNullOrWhiteSpace($pidDir)) {
        [System.IO.Directory]::CreateDirectory($pidDir) | Out-Null
    }
    [System.IO.File]::WriteAllText(
        $resolvedPidFile,
        $process.Id.ToString([System.Globalization.CultureInfo]::InvariantCulture),
        [System.Text.Encoding]::ASCII
    )
}
$stdout = $process.StandardOutput.ReadToEndAsync()
$stderr = $process.StandardError.ReadToEndAsync()

if (-not $process.WaitForExit($TimeoutSeconds * 1000)) {
    Stop-GatedProcessTree $process
    $combined = $stdout.Result + $stderr.Result
    [System.IO.File]::WriteAllText($logPath, $combined)
    [Console]::Out.Write($stdout.Result)
    [Console]::Error.Write($stderr.Result)
    [Console]::Error.WriteLine(
        "CQ_GATE_WATCHDOG_TIMEOUT: $Script exceeded ${TimeoutSeconds}s (PID $($process.Id) terminated)."
    )
    [Console]::Error.WriteLine("CQ_GATE_LOG: $logPath")
    exit 124
}

$combined = $stdout.Result + $stderr.Result
[System.IO.File]::WriteAllText($logPath, $combined)
[Console]::Out.Write($stdout.Result)
[Console]::Error.Write($stderr.Result)
$markerMatches = [regex]::Matches($combined, "(?m)^CQ_[A-Z0-9_]+\s*$")
$lastMarker = if ($markerMatches.Count -gt 0) {
    $markerMatches[$markerMatches.Count - 1].Value.Trim()
} else {
    $null
}

if ($process.ExitCode -ne 0) {
    [Console]::Error.WriteLine(
        "CQ_GATE_PROCESS_FAILED: $Script exited with code $($process.ExitCode)."
    )
    [Console]::Error.WriteLine("CQ_GATE_LOG: $logPath")
    exit $process.ExitCode
}
if (($null -ne $requiredMarker) -and ($lastMarker -ne $requiredMarker)) {
    [Console]::Error.WriteLine(
        "CQ_GATE_MARKER_MISSING: $Script exited 0 but its final marker was " +
        "'$lastMarker', expected '$requiredMarker'."
    )
    [Console]::Error.WriteLine("CQ_GATE_LOG: $logPath")
    exit 125
}

[Console]::Out.WriteLine("CQ_GATE_LOG: $logPath")
exit 0
