# resolve_sclang.ps1 -- locate sclang.exe without hardcoding one machine's path.
# ASCII ONLY: Windows PowerShell 5.1 reads a BOM-less script as ANSI, and a
# non-ASCII byte can terminate a string and break the parse far below itself.
#
# AGENTS.md: machine-specific values live in config.scd or tests/dev_paths.scd,
# both gitignored. A tracked default like "C:\Program Files\SuperCollider-3.13.0"
# is both a machine dependency and a version pin that goes stale.
#
# Resolution order, most explicit first:
#   1. $env:SC_LANG        -- the name .github/workflows/ci.yml already sets
#   2. tests/dev_paths.scd -- the gitignored home for developer paths
#   3. Program Files\SuperCollider-*\sclang.exe, newest version
#
# A caller's explicit -Sclang wins outright and never reaches this script.

[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'

if (-not [string]::IsNullOrWhiteSpace($env:SC_LANG)) {
    if (Test-Path -LiteralPath $env:SC_LANG) {
        return (Resolve-Path -LiteralPath $env:SC_LANG).Path
    }
    throw "SC_LANG is set to '$env:SC_LANG' but no file exists there."
}

$devPaths = Join-Path $PSScriptRoot 'dev_paths.scd'
if (Test-Path -LiteralPath $devPaths) {
    $text = Get-Content -LiteralPath $devPaths -Raw
    $match = [regex]::Match($text, '(?m)^\s*sclang\s*:\s*"([^"]+)"')
    if ($match.Success) {
        $candidate = $match.Groups[1].Value
        if (Test-Path -LiteralPath $candidate) {
            return (Resolve-Path -LiteralPath $candidate).Path
        }
        throw ("tests/dev_paths.scd points sclang at '$candidate' " +
               "but no file exists there.")
    }
}

$roots = @(
    (Join-Path $env:ProgramFiles 'SuperCollider-*')
)
if (${env:ProgramFiles(x86)}) {
    $roots += (Join-Path ${env:ProgramFiles(x86)} 'SuperCollider-*')
}

$installs = Get-ChildItem -Path $roots -Directory -ErrorAction SilentlyContinue |
    ForEach-Object {
        $exe = Join-Path $_.FullName 'sclang.exe'
        if (Test-Path -LiteralPath $exe) {
            $parsed = [version]'0.0'
            if ($_.Name -match 'SuperCollider-(\d+(\.\d+)*)') {
                try { $parsed = [version]$Matches[1] } catch { }
            }
            [pscustomobject]@{ Path = $exe; Version = $parsed }
        }
    } |
    Sort-Object -Property Version -Descending

if ($installs) {
    return @($installs)[0].Path
}

throw ("sclang.exe not found. Pass -Sclang <path>, set the SC_LANG environment " +
       "variable, or record the path in tests/dev_paths.scd (gitignored, see " +
       "CONTRIBUTING.md).")
