[CmdletBinding()]
param(
    [string]$MavenPath,
    [string]$JavaHome,
    [string]$EvidenceDirectory = '.tools/midterm-current-evidence'
)
$ErrorActionPreference = 'Stop'
$repoPath = Split-Path -Parent $PSScriptRoot
if (-not [IO.Path]::IsPathRooted($EvidenceDirectory)) {
    $EvidenceDirectory = Join-Path $repoPath $EvidenceDirectory
}
New-Item -ItemType Directory -Path $EvidenceDirectory -Force | Out-Null
if ($JavaHome) { $env:JAVA_HOME = $JavaHome }
if (-not $MavenPath) {
    $command = Get-Command mvn.cmd -ErrorAction SilentlyContinue
    if (-not $command) { throw 'Provide -MavenPath or install Maven on PATH.' }
    $MavenPath = $command.Source
}
function Invoke-Evidence([string]$Tool, [string[]]$ToolArguments, [string]$LogName) {
    & $Tool @ToolArguments > (Join-Path $EvidenceDirectory $LogName) 2>&1
    if ($LASTEXITCODE -ne 0) { throw "Failed: $LogName. See the evidence log." }
}
Push-Location $repoPath
try {
    $startedAt = [DateTimeOffset]::Now.ToString('o')
    $mavenBase = @('-B', "-Dmaven.repo.local=$repoPath/.tools/m2", '-f', 'backend/pom.xml')
    # Resolve the agent, then explicitly pass it to Surefire. Relative paths also
    # avoid agent path encoding issues on Windows workspaces with Chinese names.
    Invoke-Evidence $MavenPath ($mavenBase + @('org.jacoco:jacoco-maven-plugin:0.8.12:prepare-agent')) 'coverage-setup.txt'
    $agentOption = '-DargLine=-javaagent:../.tools/m2/org/jacoco/org.jacoco.agent/0.8.12/org.jacoco.agent-0.8.12-runtime.jar=destfile=target/jacoco.exec,append=false'
    Invoke-Evidence $MavenPath ($mavenBase + @('clean', $agentOption, 'test', 'org.jacoco:jacoco-maven-plugin:0.8.12:report')) 'backend-tests.txt'
    if (-not (Test-Path 'backend/target/site/jacoco/jacoco.xml')) { throw 'Coverage XML was not generated.' }
    $reports = @(Get-ChildItem 'backend/target/surefire-reports/TEST-*.xml')
    if ($reports.Count -eq 0) { throw 'JUnit XML was not generated.' }
    $suiteResults = @($reports | ForEach-Object {
        [xml]$suite = Get-Content -LiteralPath $_.FullName -Raw
        if ([int]$suite.testsuite.failures -ne 0 -or [int]$suite.testsuite.errors -ne 0) { throw "Failed suite: $($_.Name)" }
        [ordered]@{ name=$suite.testsuite.name; tests=[int]$suite.testsuite.tests; failures=[int]$suite.testsuite.failures; errors=[int]$suite.testsuite.errors; skipped=[int]$suite.testsuite.skipped }
    })
    $xmlPath = Join-Path $EvidenceDirectory 'junit'
    New-Item -ItemType Directory -Path $xmlPath -Force | Out-Null
    $reports | Copy-Item -Destination $xmlPath -Force
    Copy-Item 'backend/target/site/jacoco' (Join-Path $EvidenceDirectory 'jacoco') -Recurse -Force
    Copy-Item 'backend/target/jacoco.exec' (Join-Path $EvidenceDirectory 'jacoco.exec') -Force
    $webTests = @(Get-ChildItem 'web/tests/*.test.js' | ForEach-Object { $_.FullName })
    Invoke-Evidence 'node' (@('--test') + $webTests) 'web-tests.txt'
    Invoke-Evidence 'npm.cmd' @('--prefix','web','run','build') 'web-build.txt'
    $metadata = [ordered]@{
        startedAt=$startedAt; finishedAt=[DateTimeOffset]::Now.ToString('o'); commit=(& git rev-parse HEAD)
        javaHome=$env:JAVA_HOME; node=(& node --version); suites=$suiteResults
        testSourceSha256=(Get-FileHash 'backend/src/test/java/com/projectpandora/api/MidtermAcceptanceTests.java' -Algorithm SHA256).Hash
        caseCatalogSha256=(Get-FileHash 'backend/src/test/resources/midterm-cases.json' -Algorithm SHA256).Hash
        note='A rerun produces fresh evidence; update the written report and manual checks separately.'
    }
    $metadata | ConvertTo-Json -Depth 8 | Set-Content (Join-Path $EvidenceDirectory 'run-metadata.json') -Encoding utf8
    Write-Host "Evidence saved to $EvidenceDirectory"
} finally { Pop-Location }
