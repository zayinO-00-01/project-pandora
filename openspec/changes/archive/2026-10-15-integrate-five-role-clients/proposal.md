## Why
main 的五角色与 yukino Android 尚未兼容，旧 Web 仍是业务工作台，当前不能完整演示新角色闭环。

## What Changes
- 以 main 五角色、安全登录与账号管理为基线，保留 yukino Android 设计。
- 接入七天日志编辑、直属上级查看申请和 Android 任务派发。
- Web 只为 ADMIN 提供账号管理；兼容现有数据库且保留日志任务。

## Capabilities
### New Capabilities
- `integrated-workspace`: 五角色跨端业务整合、时间窗口、授权与数据升级。
### Modified Capabilities

## Impact
backend 权限、迁移、组织/申请/任务 API；web 管理界面；android 传输及派发；运行文档和 OpenAPI。仅本地交付，不推送远程。
