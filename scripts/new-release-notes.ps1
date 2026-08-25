[CmdletBinding()]
param(
    [Parameter(Mandatory)]
    [string]$Version,
    [string]$ChangelogPath = (Join-Path $PSScriptRoot '..\CHANGELOG.md'),
    [string]$OutputPath
)

$ErrorActionPreference = 'Stop'
$normalizedVersion = $Version.Trim()
if ($normalizedVersion.StartsWith('v', [StringComparison]::OrdinalIgnoreCase)) {
    $normalizedVersion = $normalizedVersion.Substring(1)
}
if ([string]::IsNullOrWhiteSpace($normalizedVersion)) {
    throw 'リリースバージョンが空です。'
}

$resolvedChangelog = (Resolve-Path -LiteralPath $ChangelogPath).Path
$lines = Get-Content -LiteralPath $resolvedChangelog -Encoding UTF8
$headingPattern = '^## \[' + [regex]::Escape($normalizedVersion) + '\](?: - \d{4}-\d{2}-\d{2})?$'
$matches = @(
    for ($index = 0; $index -lt $lines.Count; $index++) {
        if ($lines[$index] -match $headingPattern) { $index }
    }
)
if ($matches.Count -ne 1) {
    throw "CHANGELOGの[$normalizedVersion]節は1件必要です（検出: $($matches.Count)件）。"
}

$start = $matches[0]
$end = $lines.Count
for ($index = $start + 1; $index -lt $lines.Count; $index++) {
    if ($lines[$index] -match '^## ') {
        $end = $index
        break
    }
}
$section = @($lines[$start..($end - 1)])
while ($section.Count -gt 0 -and [string]::IsNullOrWhiteSpace($section[-1])) {
    $section = @($section[0..($section.Count - 2)])
}
if (-not ($section | Where-Object { $_ -match '^- ' })) {
    throw "CHANGELOGの[$normalizedVersion]節に変更項目がありません。"
}

$notes = ($section -join "`n") + "`n"
if ([string]::IsNullOrWhiteSpace($OutputPath)) {
    Write-Output $notes
    return
}

$resolvedOutput = [IO.Path]::GetFullPath($OutputPath)
$outputDirectory = Split-Path -Parent $resolvedOutput
if (-not (Test-Path -LiteralPath $outputDirectory)) {
    New-Item -ItemType Directory -Path $outputDirectory | Out-Null
}
[IO.File]::WriteAllText($resolvedOutput, $notes, [Text.UTF8Encoding]::new($false))
