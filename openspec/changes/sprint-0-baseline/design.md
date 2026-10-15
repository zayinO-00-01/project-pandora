# Design: sprint-0-baseline

## Context

双端（Android + Web）共享同一 Backend。Sprint 0 不实现完整业务，但需定技术边界，避免 Sprint 1 返工。

## Goals / Non-Goals

- Goals: 统一接口风格、角色模型、日志/任务主实体；目录与协作流程可执行
- Non-Goals: 选定并锁死具体框架版本以外的优化；不做 AI；不做复杂工作流引擎

## Technical Approach

### 仓库

Monorepo：

- `backend/`：REST API + DB migration
- `android/`：Kotlin App
- `web/`：SPA 管理端
- `openspec/`、`docs/`：规格与过程文档

### API 约定（草案）

- Base: `/api/v1`
- Auth: `Authorization: Bearer <token>`
- 错误体：`{ "code", "message" }`
- 时间：ISO-8601，时区 Asia/Shanghai

### Sprint 1 后端收尾更新（2026-10-15）

以下三档角色起步记录已由 `admin-web-app-clients` 的五角色规则取代：ADMIN 仅 Web；FOUNDER、DEPT_HEAD、TEAM_LEAD、STAFF 均为 App 业务用户。ADMIN 禁止日志读写/完整性/个人面板，四级业务角色禁止用户管理。401/403 在安全过滤器统一输出 JSON，业务格式错误返回 400。实际错误体字段为 `timestamp/status/error/message/path`（`ApiError`），取代初期 `code/message` 草案。

本轮沿用同日多条、创建即 submitted 的日志接口，不新增草稿或编辑；完整性接口校验权限后实际读取当日日志。组织设置检查环路及 ADMIN 隔离，更新使用事务避免失败部分写入；用户名 trim 后判重。演示初始化只补缺失数据并兼容首次旧角色链升级，不覆盖既有配置。

重启测试使用独立临时文件 H2，两次启动完整 Spring 应用上下文并在中间关闭连接池，验证日志和用户设置保留；不视为 MySQL、系统进程崩溃恢复或客户端联调已验收。自动化证据与剩余事项记录在 `backend/README.md`。

### 角色（初始简化方案，已被上述五角色方案取代）

| 存储值 | 含义 | 对应甲方五级 |
|--------|------|--------------|
| `ADMIN` | 管理员 | 管理员 |
| `LEADER` | 上级 | 创始人/部门老总/团队长（暂合并） |
| `STAFF` | 员工 | 员工 |

五级角色第一阶段不拆。创始人、部门老总、团队长都记为 `LEADER`。

### 模块边界

- P2 拥有 backend 与 OpenAPI/契约文件
- P3/P4 按 Android 包名拆分（如 `ui.log` / `ui.calendar`）减少冲突
- P5 独占 `web/src` 业务页；共享类型可从 OpenAPI 生成

### 面板与任务

- 导图上面两块（公司大事、公司派发）非管理员只读，可写个人备注；下面两块由本人修改
- 进入页面或下拉刷新拉最新数据，第一阶段不做长连接
- 任务字段：名称、详情、优先级、完成情况、时间节点、进度、进度说明、责任人、关联人
- 邀约与派发同一张 `tasks` 表，用 `source=invite|dispatch` 区分。不做多级审批

### 月相主题

- 服务端或客户端根据日期计算月相 → 映射到预定义 Theme token
- 需可单测：固定日期 → 固定主题 id

### 日志页完整性校验

- 「每日首次进入」用本地日期标记 + 服务端可选校验接口
- 校验项示例：token 有效、必填配置齐全、当日日志草稿可加载

## Decisions

1. Monorepo 便于 OpenSpec 与课设提交
2. 先三档角色，后五级
3. AI、跨公司匹配、分层复核、统计图不进第一阶段，另开 change 再做
4. 课程要验证的两项非功能定为安全性（鉴权 401/403：P1 定规则，P2 实现）与可靠性（重启后仍可查询、失败保留输入：P2 保障，P4 协调验收）。月相主题仍由 P4 实现，不占用这两项名额

## Risks

- 双端并行依赖契约：P2 须在编码首周冻结 v0 字段
- 完整性校验做在日志页入口，页面归 P3，校验规则归 P4，联调前先对接口
