[CmdletBinding()]
param(
    [Parameter(Mandatory=$true)][string]$JdbcDriverJar,
    [string]$JavaHome,
    [switch]$Apply,
    [string]$Confirm,
    [string]$BackupDirectory
)
$ErrorActionPreference='Stop'
if (-not $env:PANDORA_DEMO_JDBC_URL) { throw 'Set PANDORA_DEMO_JDBC_URL to the deployed application database explicitly.' }
$javaTool=if($JavaHome){Join-Path $JavaHome 'bin/java.exe'}elseif($env:JAVA_HOME){Join-Path $env:JAVA_HOME 'bin/java.exe'}else{'java.exe'}
$toolArguments=@('--preview')
if($Apply) {
    if($Confirm -cne 'RESET-DEMO-DATA' -or -not $BackupDirectory){throw 'Apply requires -Confirm RESET-DEMO-DATA and -BackupDirectory.'}
    $toolArguments=@('--apply','--confirm',$Confirm,'--backup-dir',[IO.Path]::GetFullPath($BackupDirectory))
}
& $javaTool '-Dfile.encoding=UTF-8' '--source' '17' '--class-path' (Resolve-Path -LiteralPath $JdbcDriverJar).Path (Join-Path $PSScriptRoot 'ResetDemoData.java') @toolArguments
if($LASTEXITCODE -ne 0){throw 'Reset tool failed. Read its output; do not restart until the database state has been confirmed.'}
