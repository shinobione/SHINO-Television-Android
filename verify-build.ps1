param([string]$ArtifactTag = 'v0.2', [string]$Version = '0.2.0', [int]$VersionCode = 2)
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$tools = Get-Content -LiteralPath (Join-Path $root '.local-build/tool-paths.json') -Raw | ConvertFrom-Json
$env:JAVA_HOME = $tools.JavaHome
$sdk = Join-Path $root '.local-build/android-sdk'
$bt = Join-Path $sdk 'build-tools/35.0.0'
$apk = Join-Path $root 'app/build/outputs/apk/debug/app-debug.apk'
$out = Join-Path $root "artifacts/$ArtifactTag"
New-Item -ItemType Directory -Force $out | Out-Null
$env:PATH = (Join-Path $env:JAVA_HOME 'bin') + ';' + $env:PATH
$signature = & (Join-Path $bt 'apksigner.bat') verify --verbose --print-certs $apk 2>&1
if ($LASTEXITCODE -ne 0) { throw 'APK signature failed' }
$signature | Set-Content -LiteralPath (Join-Path $out 'signature.txt')
& (Join-Path $bt 'zipalign.exe') -c -P 16 -v 4 $apk | Set-Content -LiteralPath (Join-Path $out 'alignment.txt')
if ($LASTEXITCODE -ne 0) { throw 'APK alignment failed' }
$badging = & (Join-Path $bt 'aapt.exe') dump badging $apk
if ($LASTEXITCODE -ne 0) { throw 'APK metadata failed' }
if (($badging -join "`n") -notmatch ("versionCode='" + $VersionCode + "' versionName='" + [regex]::Escape($Version) + "'")) { throw 'Packaged version differs from requested delivery version' }
$badging | Set-Content -LiteralPath (Join-Path $out 'apk-metadata.txt')
$manifest = & (Join-Path $bt 'aapt.exe') dump xmltree $apk AndroidManifest.xml
if ($LASTEXITCODE -ne 0) { throw 'APK manifest failed' }
if ($manifest -match 'android.intent.category.HOME') { throw 'Unexpected HOME activity' }
if (-not ($manifest -match 'android.intent.category.LEANBACK_LAUNCHER')) { throw 'Missing TV activity' }
if ($manifest -match 'android.accessibilityservice.AccessibilityService') { throw 'Unexpected accessibility service' }
$manifest | Set-Content -LiteralPath (Join-Path $out 'apk-manifest.txt')
$dest = Join-Path $out "SHINO-TV-HOME-$ArtifactTag-debug.apk"
Copy-Item -LiteralPath $apk -Destination $dest -Force
$testApk = Join-Path $root 'app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk'
if (Test-Path -LiteralPath $testApk) { Copy-Item -LiteralPath $testApk -Destination (Join-Path $out "SHINO-TV-HOME-$ArtifactTag-androidTest.apk") -Force }
$logoOriginal = (Get-FileHash -LiteralPath (Join-Path $root 'logo SHINOBIWAN.png') -Algorithm SHA256).Hash
$logoApp = (Get-FileHash -LiteralPath (Join-Path $root 'app/src/main/res/drawable-nodpi/brand.png') -Algorithm SHA256).Hash
if ($logoOriginal -ne $logoApp) { throw 'Official logo changed' }
$result = [ordered]@{
    package = 'fr.shinobiwan.tvhome'; version = $Version; versionCode = $VersionCode
    bytes = (Get-Item -LiteralPath $dest).Length
    sha256 = (Get-FileHash -LiteralPath $dest -Algorithm SHA256).Hash.ToLowerInvariant()
    signatureVerified = $true; alignment16KiBVerified = $true
    leanback = $true; home = $false; accessibilityService = $false
    officialLogoSha256 = $logoApp.ToLowerInvariant(); officialLogoUnchanged = $true
    physicalDeviceTested = $false; instrumentationExecuted = $false
}
$result | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $out 'apk-verification.json')
$result | ConvertTo-Json
