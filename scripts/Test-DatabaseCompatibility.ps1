[CmdletBinding()]
param([string]$JavaHome,[switch]$SafetyOnly)
$ErrorActionPreference='Stop'
$repoPath=Split-Path -Parent $PSScriptRoot
if(-not $JavaHome){
    if($env:JAVA_HOME){$JavaHome=$env:JAVA_HOME}
    elseif(Test-Path 'E:/Program Files/JetBrains/IntelliJ IDEA Community Edition 2025.1.3/jbr/bin/java.exe'){$JavaHome='E:/Program Files/JetBrains/IntelliJ IDEA Community Edition 2025.1.3/jbr'}
    else{$javaCommand=Get-Command java.exe -ErrorAction Stop;$JavaHome=Split-Path -Parent (Split-Path -Parent $javaCommand.Source)}
}
$compileDir=Join-Path $repoPath '.tools/compat-test-classes'
New-Item -ItemType Directory -Force -Path $compileDir | Out-Null
$jars=@(Get-ChildItem -LiteralPath (Join-Path $repoPath '.tools/m2') -Recurse -Filter '*.jar' | Where-Object {$_.Name -notmatch '(sources|javadoc)'} | ForEach-Object {$_.FullName})
$classPath=(@($compileDir)+$jars) -join [System.IO.Path]::PathSeparator
& (Join-Path $JavaHome 'bin/javac.exe') '-encoding' 'UTF-8' '-cp' $classPath '-d' $compileDir (Join-Path $PSScriptRoot 'UpgradeDemoDatabase.java') (Join-Path $PSScriptRoot 'UpgradeDemoDatabaseTest.java')
if($LASTEXITCODE -ne 0){throw 'Compatibility regression compilation failed.'}
& (Join-Path $JavaHome 'bin/java.exe') '-Dfile.encoding=UTF-8' '-cp' $classPath 'UpgradeDemoDatabaseTest' $repoPath $(if($SafetyOnly){'safety-only'}else{'full'})
if($LASTEXITCODE -ne 0){throw 'Compatibility regression failed.'}