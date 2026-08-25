[CmdletBinding()]
param(
    [Parameter(Mandatory)][string]$Repository,
    [Parameter(Mandatory)][string]$Tag,
    [Parameter(Mandatory)][string]$Version,
    [Parameter(Mandatory)][string]$CreatedAt,
    [Parameter(Mandatory)][string]$OutputPath
)

$ErrorActionPreference = 'Stop'
$utc = [DateTimeOffset]::Parse($CreatedAt).UtcDateTime.ToString('yyyy-MM-ddTHH:mm:ss')
$manifest = [ordered]@{
    download_url = "https://github.com/$Repository/releases/download/$Tag/patches.rvp"
    created_at = $utc
    description = 'Nicomanga向けReVanced Patches'
    version = $Version
}
$manifest | ConvertTo-Json | Set-Content -LiteralPath $OutputPath -Encoding utf8NoBOM
