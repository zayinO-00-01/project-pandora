[CmdletBinding()]
param(
    [string]$BaseUrl = 'http://localhost:8080',
    [string]$Username = 'staff',
    [string]$Password = 'demo1234',
    [string]$Content = '今日完成接口联调，下一步连接 Android。',
    [string]$Date = (Get-Date -Format 'yyyy-MM-dd'),
    [long]$LogId = 0,
    [switch]$Draft,
    [switch]$Submit
)
$ErrorActionPreference = 'Stop'
$apiBase = $BaseUrl.TrimEnd('/') + '/api/v1'
function Send-Json([string]$Path, [string]$Method, $Body, $Headers) {
    $request = @{ Uri="$apiBase$Path"; Method=$Method; Headers=$Headers; ContentType='application/json; charset=utf-8' }
    if ($null -ne $Body) { $request.Body=[System.Text.Encoding]::UTF8.GetBytes(($Body | ConvertTo-Json -Compress)) }
    Invoke-RestMethod @request
}
try {
    if ($Draft -and $Submit) { throw '-Draft 与 -Submit 不能一起使用。' }
    if ($Submit -and $LogId -le 0) { throw '提交已有草稿需要指定 -LogId。' }
    $user = Send-Json '/auth/login' 'POST' @{username=$Username; password=$Password} @{}
    $headers = @{ Authorization="Bearer $($user.token)" }
    if ($Submit) { $result = Send-Json "/logs/$LogId/submit" 'POST' $null $headers }
    else {
        $body = @{logDate=$Date; content=$Content; status=$(if ($Draft) {'draft'} else {'submitted'})}
        if ($LogId -gt 0) { $result=Send-Json "/logs/$LogId" 'PUT' $body $headers }
        else { $result=Send-Json '/logs' 'POST' $body $headers }
    }
    $result | Format-List id,userId,logDate,content,status,updatedAt
    Write-Host "日志 #$($result.id) 已保存。管理端选择该员工后刷新。"
    Write-Host "修改：.\scripts\Invoke-EmployeeDemo.ps1 -LogId $($result.id) -Content '新的工作进度'"
    if ($result.status -eq 'draft') { Write-Host "提交：.\scripts\Invoke-EmployeeDemo.ps1 -LogId $($result.id) -Submit" }
} catch { Write-Host "操作失败：$($_.Exception.Message)" -ForegroundColor Red; exit 1 }
