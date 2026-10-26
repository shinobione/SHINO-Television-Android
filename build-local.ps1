param([string[]]$Tasks = @(':app:testDebugUnitTest', ':app:assembleDebug', ':app:lintDebug'))
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$config = Get-Content -LiteralPath (Join-Path $root '.local-build/tool-paths.json') -Raw | ConvertFrom-Json
$env:JAVA_HOME = $config.JavaHome
$env:ANDROID_HOME = Join-Path $root '.local-build/android-sdk'
$env:ANDROID_SDK_ROOT = $env:ANDROID_HOME
$env:GRADLE_USER_HOME = Join-Path $root '.local-build/gradle-user-home'
$env:ANDROID_USER_HOME = Join-Path $root '.local-build/android-user-home'
$env:TEMP = Join-Path $root '.local-build/tmp'
$env:TMP = $env:TEMP
New-Item -ItemType Directory -Force $env:GRADLE_USER_HOME,$env:ANDROID_USER_HOME,$env:TEMP,(Join-Path $root '.local-build/logs') | Out-Null
Push-Location -LiteralPath $root
try {
    & (Join-Path $config.GradleHome 'bin/gradle.bat') --no-daemon --console=plain @Tasks 2>&1 | Tee-Object -FilePath '.local-build/logs/gradle.log'
    $buildExit = $LASTEXITCODE
    if ($buildExit -ne 0) { throw "Gradle exit $buildExit" }
} finally { Pop-Location }
