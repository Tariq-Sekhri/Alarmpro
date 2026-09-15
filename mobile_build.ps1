$ErrorActionPreference = 'Stop'

$projectRoot = (Resolve-Path -LiteralPath $PSScriptRoot).Path
Set-Location -LiteralPath $projectRoot

$keystorePropertiesPath = Join-Path $projectRoot 'keystore.properties'
if (!(Test-Path -LiteralPath $keystorePropertiesPath)) {
    throw 'Missing keystore.properties. Copy the local release signing configuration before building.'
}

$properties = ConvertFrom-StringData -StringData (
    (Get-Content -LiteralPath $keystorePropertiesPath | Where-Object {
        $_.Trim() -and -not $_.TrimStart().StartsWith('#')
    }) -join "`n"
)
$requiredKeys = 'storeFile', 'storePassword', 'keyAlias', 'keyPassword'
$missingKeys = $requiredKeys | Where-Object { [string]::IsNullOrWhiteSpace($properties[$_]) }
if ($missingKeys) {
    throw "keystore.properties is missing: $($missingKeys -join ', ')"
}

$keystorePath = $properties.storeFile
if (!(Split-Path -IsAbsolute $keystorePath)) {
    $keystorePath = Join-Path $projectRoot $keystorePath
}
if (!(Test-Path -LiteralPath $keystorePath)) {
    throw 'Configured release keystore file was not found.'
}

Write-Host 'Building signed AlarmPro Android App Bundle...'
& (Join-Path $projectRoot 'gradlew.bat') bundleRelease --console=plain
if ($LASTEXITCODE -ne 0) {
    throw "Gradle bundleRelease failed with exit code $LASTEXITCODE"
}

$aabPaths = @(
    (Join-Path $projectRoot 'app\build\outputs\bundle\release\app-release.aab'),
    (Join-Path $env:LOCALAPPDATA 'Alarmpro-build-active\app\outputs\bundle\release\app-release.aab')
)
$aabPath = $aabPaths | Where-Object { Test-Path -LiteralPath $_ } |
    Sort-Object { (Get-Item -LiteralPath $_).LastWriteTimeUtc } -Descending |
    Select-Object -First 1
if (!$aabPath) {
    throw 'Signed release AAB was not produced.'
}

$aab = Get-Item -LiteralPath $aabPath
$releaseDirectory = Join-Path $projectRoot 'releases\release'
New-Item -ItemType Directory -Force -Path $releaseDirectory | Out-Null
$publishedAabPath = Join-Path $releaseDirectory 'app-release.aab'
Copy-Item -LiteralPath $aab.FullName -Destination $publishedAabPath -Force

$publishedAab = Get-Item -LiteralPath $publishedAabPath
Write-Host "Signed release AAB ready: $($publishedAab.FullName) ($([math]::Round($publishedAab.Length / 1MB, 2)) MB)"

$nativeLibsPaths = @(
    (Join-Path $projectRoot 'app\build\intermediates\merged_native_libs\release\mergeReleaseNativeLibs\out\lib'),
    (Join-Path $env:LOCALAPPDATA 'Alarmpro-build-active\app\intermediates\merged_native_libs\release\mergeReleaseNativeLibs\out\lib')
)
$nativeLibsPath = $nativeLibsPaths | Where-Object { Test-Path -LiteralPath $_ } | Select-Object -First 1
if ($nativeLibsPath) {
    $publishedSymbolsPath = Join-Path $releaseDirectory 'native-debug-symbols.zip'
    Compress-Archive -Path (Join-Path $nativeLibsPath '*') -DestinationPath $publishedSymbolsPath -Force
    Write-Host "Native debug symbols ready: $publishedSymbolsPath"
}
