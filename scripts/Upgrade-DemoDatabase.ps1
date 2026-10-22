[CmdletBinding()]
param([string]$DatabasePath,[string]$JavaHome,[string]$H2Jar)
$ErrorActionPreference='Stop'
$repoPath=Split-Path -Parent $PSScriptRoot
if(Test-Path (Join-Path $repoPath 'backend/src/main/resources/db/migration/V3__dispatch_tasks.sql')){
    Write-Host '本分支保留原有 V3 tasks / V4 view requests / V5 roles 迁移顺序，由 Flyway 按原序升级。'
    return
}
if(-not $DatabasePath){$DatabasePath=Join-Path $repoPath 'backend/data/pandora'}
if(-not (Test-Path -LiteralPath "$DatabasePath.mv.db")){Write-Host '数据库尚不存在，首次启动由 Flyway 创建。';return}
if(-not $JavaHome){
    if($env:JAVA_HOME){$JavaHome=$env:JAVA_HOME}
    elseif(Test-Path 'E:/Program Files/JetBrains/IntelliJ IDEA Community Edition 2025.1.3/jbr/bin/java.exe'){$JavaHome='E:/Program Files/JetBrains/IntelliJ IDEA Community Edition 2025.1.3/jbr'}
    else{$javaCommand=Get-Command java.exe -ErrorAction Stop;$JavaHome=Split-Path -Parent (Split-Path -Parent $javaCommand.Source)}
}
if(-not $H2Jar){
    foreach($root in @((Join-Path $repoPath '.tools/m2/com/h2database/h2'),(Join-Path $env:USERPROFILE '.m2/repository/com/h2database/h2'))){
        if(Test-Path -LiteralPath $root){$candidate=Get-ChildItem -LiteralPath $root -Recurse -Filter 'h2-*.jar' | Where-Object {$_.Name -notmatch '(sources|javadoc)'} | Sort-Object FullName -Descending | Select-Object -First 1;if($candidate){$H2Jar=$candidate.FullName;break}}
    }
}
if(-not $H2Jar){throw '未找到 H2 JDBC。请先构建后端，或传入 -H2Jar。'}
& (Join-Path $JavaHome 'bin/java.exe') '-Dfile.encoding=UTF-8' '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' '--class-path' $H2Jar (Join-Path $PSScriptRoot 'UpgradeDemoDatabase.java') ([System.IO.Path]::GetFullPath($DatabasePath)) (Join-Path $repoPath 'backend/src/main/resources/db/migration')
if($LASTEXITCODE -ne 0){throw '数据库升级未完成；请停止演示服务并查看上方原因；如已产生备份请保留，不要删除数据或执行全局 repair。'}
