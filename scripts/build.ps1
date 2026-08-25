[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$repositoryRoot = Split-Path -Parent $PSScriptRoot
$sdk = $env:ANDROID_HOME
if ([string]::IsNullOrWhiteSpace($sdk)) {
    $candidate = Join-Path $env:LOCALAPPDATA 'Android\Sdk'
    if (Test-Path -LiteralPath $candidate) { $sdk = $candidate }
}
if ([string]::IsNullOrWhiteSpace($sdk) -or -not (Test-Path -LiteralPath $sdk)) {
    throw 'Android SDKが見つかりません。ANDROID_HOMEを設定してください。'
}

$env:ANDROID_HOME = $sdk
$env:ANDROID_SDK_ROOT = $sdk
$env:ORG_GRADLE_PROJECT_githubPackagesUsername = (& gh api user --jq .login)
$env:ORG_GRADLE_PROJECT_githubPackagesPassword = (& gh auth token)

Push-Location $repositoryRoot
try {
    & .\gradlew.bat :patches:buildAndroid --no-daemon
    if ($LASTEXITCODE -ne 0) { throw "Gradleビルドに失敗しました（exit code: $LASTEXITCODE）" }
} finally {
    Pop-Location
}
