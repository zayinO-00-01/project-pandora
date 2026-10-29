[CmdletBinding()]
param()

$ErrorActionPreference = 'Stop'
$repository = Split-Path -Parent $PSScriptRoot
$tracked = @(& git -C $repository -c core.quotepath=false ls-files)
if ($LASTEXITCODE -ne 0) {
    throw 'Cannot read the Git file list.'
}

# Match path components, not words in business documents or source code.
$forbidden = '(?i)(^|/)(\.(cursor|workbuddy|agents|codex|claude|windsurf|trae|qoder|kiro|idea|vscode|tools|local-archive|gradle|kotlin)(/|$)|node_modules(/|$)|(AGENTS|CLAUDE|GEMINI)\.md$|\.cursorrules$|\.cursorignore$|\.windsurfrules$)'
$unexpected = @($tracked | Where-Object { $_ -match $forbidden })
if ($unexpected.Count -gt 0) {
    Write-Host 'Local tool files must not be tracked in the project:'
    $unexpected | ForEach-Object { Write-Host "  $_" }
    Write-Host 'Remove them from Git tracking; .gitignore alone does not remove tracked files.'
    exit 1
}
Write-Host 'Repository content check passed: no tracked local tool configuration or caches.'
