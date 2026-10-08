[CmdletBinding()]
param([string]$SdkPath,[string]$JavaHome,[switch]$RunTests)
$ErrorActionPreference='Stop'
$repoPath=Split-Path -Parent $PSScriptRoot
$toolPath=Join-Path $repoPath '.tools'
function Invoke-Checked([string]$Tool,[string[]]$Arguments) {
    & $Tool @Arguments
    if($LASTEXITCODE -ne 0){throw "命令失败（$LASTEXITCODE）：$Tool"}
}
try {
    New-Item -ItemType Directory -Path $toolPath -Force | Out-Null
    if(-not $SdkPath){
        foreach($candidate in @((Join-Path $toolPath 'android-sdk'),$env:ANDROID_HOME,$env:ANDROID_SDK_ROOT,(Join-Path $env:LOCALAPPDATA 'Android/Sdk'))){
            if($candidate -and (Test-Path -LiteralPath (Join-Path $candidate 'platforms/android-35/android.jar'))){$SdkPath=$candidate;break}
        }
    }
    if(-not $SdkPath){throw '未找到 Android SDK 35，请用 Android Studio 安装并传入 -SdkPath。'}
    if(-not $JavaHome){
        $ideaJava='E:/Program Files/JetBrains/IntelliJ IDEA Community Edition 2025.1.3/jbr'
        if(Test-Path -LiteralPath "$ideaJava/bin/java.exe"){$JavaHome=$ideaJava}
        elseif($env:JAVA_HOME){$JavaHome=$env:JAVA_HOME}
        else{$javaCommand=Get-Command java.exe -ErrorAction Stop;$JavaHome=Split-Path -Parent (Split-Path -Parent $javaCommand.Source)}
    }
    $env:JAVA_HOME=$JavaHome
    $env:GRADLE_USER_HOME=Join-Path $toolPath 'gradle-cache'
    $javaPath=Join-Path $JavaHome 'bin/java.exe'
    $javaInfo=& $javaPath -XshowSettings:properties -version 2>&1 | ForEach-Object {$_.ToString()}
    $nativeEncoding='UTF-8'
    foreach($line in $javaInfo){if($line -match 'native.encoding\s*=\s*(\S+)'){$nativeEncoding=$Matches[1]}}
    # Windows Java parses @argfiles using native encoding. Match Gradle's writer for Chinese paths.
    $jvmSetting="-Dorg.gradle.jvmargs=-Xmx2048m -Dfile.encoding=$nativeEncoding"
    $sdkProperty=$SdkPath.Replace('\','/').Replace(':','\:')
    [System.IO.File]::WriteAllText((Join-Path $repoPath 'android/local.properties'),"sdk.dir=$sdkProperty`n",[System.Text.UTF8Encoding]::new($false))
    $keystorePath=Join-Path $toolPath 'android-debug.keystore'
    if(-not (Test-Path -LiteralPath $keystorePath)){
        Invoke-Checked (Join-Path $JavaHome 'bin/keytool.exe') @('-genkeypair','-keystore',$keystorePath,'-alias','androiddebugkey','-storepass','android','-keypass','android','-keyalg','RSA','-keysize','2048','-validity','10000','-dname','CN=Android Debug,O=Android,C=US')
    }
    $gradlePath=Join-Path $toolPath 'gradle-8.9/bin/gradle.bat'
    if(-not (Test-Path -LiteralPath $gradlePath)){$gradlePath=Join-Path $repoPath 'android/gradlew.bat'}
    $arguments=@('-p',(Join-Path $repoPath 'android'),$jvmSetting,'assembleDebug','--no-daemon','--console=plain')
    if($RunTests){$arguments+='testDebugUnitTest'}
    Invoke-Checked $gradlePath $arguments
    $deliveryPath=Join-Path $repoPath 'deliverables'
    New-Item -ItemType Directory -Path $deliveryPath -Force | Out-Null
    $apkPath=Join-Path $deliveryPath 'Pandora-MVP-debug.apk'
    Copy-Item -LiteralPath (Join-Path $repoPath 'android/app/build/outputs/apk/debug/app-debug.apk') -Destination $apkPath -Force
    Write-Host "APK 构建完成：$apkPath" -ForegroundColor Green
} catch {Write-Host "构建失败：$($_.Exception.Message)" -ForegroundColor Red;exit 1}
