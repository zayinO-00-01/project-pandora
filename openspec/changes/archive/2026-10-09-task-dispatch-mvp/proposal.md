## Why
首个日志 MVP 已完成，并由用户在 MuMu 手测未发现问题。用户已选择下一步开发任务派发和进度闭环，让原型可以演示领导布置工作、员工反馈和管理查看。
## What Changes
- Web 领导/管理员创建任务并指定责任人、优先级、截止时间及详情，刷新查看进度。
- Android 查看授权任务、更新本人任务的进度和说明；首页派发板块展示可查看的最新十条任务。
- 新增任务持久化、权限校验与 API；不包含邀约请示、复杂审批、任务删除/重新分配、通知或图表。
## Capabilities
### New Capabilities
- `task-dispatch`: 可演示的真实任务派发与进度反馈。
### Modified Capabilities
无（既有日志行为保持，新增任务区域）。
## Impact
backend 新任务模块及 Flyway V3，web 任务组件，android 任务页与数据层，OpenAPI 与简短演示说明。当前本地 H2 数据迁移不删除旧日志，继续支持 MySQL。
