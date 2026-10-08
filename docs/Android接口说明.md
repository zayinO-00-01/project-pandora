# Android 联调接口

沿用现有 Android 项目，本轮没有修改 Android。管理 Web 与 API 在同一个本地服务。

## 地址与登录

- 电脑浏览器：`http://localhost:8080`
- Android 官方模拟器：`http://10.0.2.2:8080`
- 真机：`http://电脑局域网IPv4:8080`，同一 Wi-Fi，电脑端口需允许访问。
- 全部接口前缀 `/api/v1`。Debug 包需 INTERNET 权限，并配置本地 HTTP 访问；不要在正式包全局开放明文连接。

```http
POST /api/v1/auth/login
Content-Type: application/json

{"username":"staff","password":"demo1234"}
```

响应包含 `token`、`userId`、`role`、`displayName`。后续请求带 `Authorization: Bearer <token>`。

## 日志流程

| 操作 | 方法和路径 | 说明 |
|---|---|---|
| 保存草稿 | POST /logs | body 的 status=draft，201 返回含 id 的日志 |
| 新建并提交 | POST /logs | status=submitted；省略 status 也默认提交 |
| 我的日志 | GET /logs | 包含自己的草稿；可加 ?date=2026-10-08 |
| 修改日志 | PUT /logs/{id} | 作者可修改日期/正文；提交后仍能编辑 |
| 提交已有草稿 | POST /logs/{id}/submit | 不需要 body；重复提交仍是同一条记录 |
| 可查看的成员 | GET /users | 员工只返回自己，领导返回自己及直属下属，管理员返回全部 |
| 领导查看员工 | GET /logs?userId={id} | 其他人的草稿不会返回；员工不能查看别人 |

POST /logs 和 PUT /logs/{id} 的 JSON：

```json
{"logDate":"2026-10-08","content":"完成接口联调，下一步接入日志页。","status":"draft"}
```

日期用实际日期且不能晚于今天；正文非空，最多 5000 字。PUT 省略 status 时保留原状态；已提交日志不能改回草稿。新建时不要传 userId，作者来自登录身份。

响应示例（ID 和时间以实际返回为准）：

```json
{"id":1,"userId":3,"logDate":"2026-10-08","content":"完成接口联调","status":"submitted","createdAt":"2026-10-08T02:00:00Z","updatedAt":"2026-10-08T02:05:00Z"}
```

400 参数错误；401 重新登录；403 无权访问；404 日志不存在。错误响应的 `message` 可用于提示。保存失败保留输入；成功后按返回的 id 更新本地列表，避免重复创建。网络请求放后台，服务地址应可配置。

详细契约见 `backend/openapi.yaml`。`/logs/integrity` 是历史占位接口，`/panels/map` 只有今日日志有真实数据；任务派发、AI 和完整日历尚未实现，不能按已有可用接口对接。
