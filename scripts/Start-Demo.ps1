[CmdletBinding()]
param(
    [ValidateRange(1,65535)][int]$Port = 8080,
    [switch]$SkipBuild,
    [switch]$BuildOnly,
    [string]$MavenPath,
    [string]$JavaHome
)
$ErrorActionPreference = 'Stop'
$repoPath = Split-Path -Parent $PSScriptRoot
$cachePath = Join-Path $repoPath '.tools'
function Invoke-Checked([string]$Tool, [string[]]$Arguments) {
    & $Tool @Arguments
    if ($LASTEXITCODE -ne 0) { throw "命令执行失败（$LASTEXITCODE）：$Tool" }
}
function Resolve-Tool([string]$Name, [string[]]$Candidates) {
    $found = Get-Command $Name -ErrorAction SilentlyContinue
    if ($found) { return $found.Source }
    foreach ($candidate in $Candidates) { if (Test-Path -LiteralPath $candidate) { return $candidate } }
    throw "未找到 $Name，请安装并加入 PATH，或使用脚本参数指定位置。"
}
try {
    if ($JavaHome) { $env:JAVA_HOME = $JavaHome }
    $javaCandidates = @()
    if ($env:JAVA_HOME) { $javaCandidates += (Join-Path $env:JAVA_HOME 'bin/java.exe') }
    $javaPath = Resolve-Tool 'java.exe' $javaCandidates
    if ($JavaHome) { $javaPath = Join-Path $JavaHome 'bin/java.exe' }
    if (-not (Test-Path -LiteralPath $javaPath)) { throw "Java 路径不存在：$javaPath" }
    $env:JAVA_HOME = Split-Path -Parent (Split-Path -Parent $javaPath)
    $jarPath = Join-Path $repoPath 'backend/target/pandora-api-0.1.0-SNAPSHOT.jar'
    if (-not $BuildOnly) {
        $probe = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::IPv6Any, $Port)
        try { $probe.Server.DualMode = $true; $probe.Start() }
        catch { throw "端口 $Port 已被占用。停止之前的演示服务，或运行 start-demo.cmd -Port 8081。" }
        finally { $probe.Stop() }
    }
    if (-not $SkipBuild) {
        $npmPath = Resolve-Tool 'npm.cmd' @('E:/Program Files/nodejs/npm.cmd')
        if (-not $MavenPath) {
            $MavenPath = Resolve-Tool 'mvn.cmd' @('E:/Program Files/JetBrains/IntelliJ IDEA Community Edition 2025.1.3/plugins/maven/lib/maven3/bin/mvn.cmd')
        }
        New-Item -ItemType Directory -Path $cachePath -Force | Out-Null
        Push-Location (Join-Path $repoPath 'web')
        try {
            $lockHash = (Get-FileHash -LiteralPath 'package-lock.json' -Algorithm SHA256).Hash
            $stampPath = 'node_modules/.pandora-lock-sha'
            $cachedHash = if (Test-Path -LiteralPath $stampPath) { (Get-Content -LiteralPath $stampPath -Raw).Trim() } else { '' }
            if ($cachedHash -ne $lockHash) {
                Invoke-Checked $npmPath @('ci', '--cache', (Join-Path $cachePath 'npm'), '--no-audit', '--no-fund')
                Set-Content -LiteralPath $stampPath -Value $lockHash -Encoding Ascii
            }
            Invoke-Checked $npmPath @('run','build')
        } finally { Pop-Location }
        $staticPath = Join-Path $repoPath 'backend/src/main/resources/static'
        New-Item -ItemType Directory -Path $staticPath -Force | Out-Null
        Copy-Item -Path (Join-Path $repoPath 'web/dist/*') -Destination $staticPath -Recurse -Force
        Invoke-Checked $MavenPath @('-B', "-Dmaven.repo.local=$cachePath/m2", '-f', (Join-Path $repoPath 'backend/pom.xml'), 'package', '-DskipTests')
    }
    if (-not (Test-Path -LiteralPath $jarPath)) { throw '未找到演示包，请先去掉 -SkipBuild 参数重新运行。' }
    if ($BuildOnly) { Write-Host "构建完成：$jarPath" -ForegroundColor Green; exit 0 }
    Write-Host "`n打开管理工作台：http://localhost:$Port" -ForegroundColor Green
    Write-Host '账号 leader / admin，密码 demo1234；按 Ctrl+C 停止。'
    $lanAddresses = Get-NetIPAddress -AddressFamily IPv4 -ErrorAction SilentlyContinue | Where-Object { $_.IPAddress -notmatch '^(127\.|169\.254\.)' -and $_.AddressState -eq 'Preferred' }
    foreach ($address in $lanAddresses) { Write-Host "局域网：http://$($address.IPAddress):$Port（手机需与电脑同网）" }
    Push-Location (Join-Path $repoPath 'backend')
    try { Invoke-Checked $javaPath @('-Duser.timezone=Asia/Shanghai', '-jar', $jarPath, "--server.port=$Port", '--server.address=0.0.0.0') }
    finally { Pop-Location }
} catch {
    Write-Host "启动失败：$($_.Exception.Message)" -ForegroundColor Red
    exit 1
}
