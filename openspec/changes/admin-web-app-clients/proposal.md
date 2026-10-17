## Why

甲方已确认产品边界调整：Web 仅供系统管理员做账号与组织授权；业务角色在 Android App 操作。App 角色按甲方 PDF 第 6 页定为四级（员工、团队长、部门老总、创始人），与独立的 Web `ADMIN` 并存。原「上级在 Web 看下属日志」取消。

## What Changes

- 角色模型：`ADMIN`（仅 Web）+ App 四级 `STAFF` / `TEAM_LEAD` / `DEPT_HEAD` / `FOUNDER`
- 汇报链：员工 → 团队长 → 部门老总 → 创始人（`manager_id` 指向上一级）
- Web：仅 `ADMIN` 登录；建账号、改角色、设上级、软停用；不做业务日志/派发
- App：`STAFF`/`TEAM_LEAD`/`DEPT_HEAD`/`FOUNDER`；管理员不使用 App
- 权限对齐 PDF 第 6 页：
  - `ADMIN`：全量修改系统信息（账号/组织等；分层复核仍不进第一阶段）
  - `FOUNDER`：全量穿透查看（可查看公司范围内人员日志/面板数据）、收集分发类能力按后续迭代
  - `DEPT_HEAD` / `TEAM_LEAD`：基础功能 + 向下穿透查看 + 向上邀约请示
  - `STAFF`：基础功能，仅本人信息
- 关闭对公开放注册；查日志 API 供 App 使用（按角色穿透范围）
- 更新产品/分工/页面清单：取消 Web 看下属日志

## Capabilities

### New Capabilities

- `user-admin`: 管理员用户生命周期与组织关系（含四级 App 角色赋值）

### Modified Capabilities

- `auth`: 五角色；公开注册收权；停用不可登录；按角色约束端与穿透范围

## Impact

- Affected: backend 角色枚举与授权、`openapi.yaml`、种子账号；Web/Android 客户端边界
- Out of scope here: 分层复核、跨公司匹配、任务派发完整实现（按原 Sprint 往后）
