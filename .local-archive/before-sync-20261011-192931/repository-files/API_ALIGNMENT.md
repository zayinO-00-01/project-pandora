# Pandora API 对齐说明

来源：仓库 `backend/openapi.yaml` v0，以及 `openspec/changes/sprint-0-baseline`。

## Android 已对齐

- 基础路径由 Gradle 属性 `pandoraApiBaseUrl` 注入，默认值为 `/api/v1` 的占位域名，不在代码中写死服务器 IP。
- 登录：`POST /api/v1/auth/login`，请求 `username/password`，响应 `token/role/displayName`。
- 鉴权：业务请求使用 `Authorization: Bearer <token>`。
- 日志首次进入校验：`GET /api/v1/logs/integrity`。
- 日志列表：`GET /api/v1/logs?date=YYYY-MM-DD&userId=<id>`。
- 新建日志：`POST /api/v1/logs`，请求 `logDate/content`。
- 面板聚合预留：`GET /api/v1/panels/map`。
- 任务路径已保留为 `/api/v1/tasks`，但按仓库约定属于 Sprint 2，当前不假装已完成真实联调。

## 本地配置

构建时传入：

```text
gradlew assembleDebug -PpandoraApiBaseUrl=https://your-domain.example/api/v1
```

未配置真实域名时，客户端保留本地 Demo 回退，便于继续验收 UI；配置后由 `PandoraApi` 负责网络请求和字段解析。
