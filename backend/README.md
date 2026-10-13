# Backend API

智能掌上工作系统 · 后端服务（Spring Boot 3 / Java 17）。

契约：[`openapi.yaml`](./openapi.yaml)。当前已实现：登录/注册、日志草稿/提交/修改与授权成员查询、完整性校验占位、任务派发/责任人进度反馈、面板读取（授权派发与今日日志）、健康检查。

## 本地最快启动（无需 Docker / MySQL）

默认使用本地 H2 文件库（`backend/data/pandora.*`），Flyway 自动建表并写入演示账号。

```bash
cd backend
# 需要本机已安装 Maven，或使用 IDE 运行 PandoraApplication
mvn spring-boot:run
```

健康检查：

```bash
curl http://localhost:8080/api/v1/health
```

## 演示账号

| 用户名 | 密码 | 角色 | 说明 |
|--------|------|------|------|
| `staff` | `demo1234` | STAFF | 员工；上级为 leader |
| `leader` | `demo1234` | LEADER | 可查直属员工日志 |
| `admin` | `demo1234` | ADMIN | 可查全部 |

完整演示使用根目录 `start-demo.cmd`；员工脚本与步骤见 [本地演示](../docs/本地演示.md)，手机对接见 [Android接口说明](../docs/Android接口说明.md)。

## 联调脚本（PowerShell）

```powershell
# 登录
$login = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/auth/login `
  -ContentType application/json -Body '{"username":"staff","password":"demo1234"}'
$token = $login.token

# 写日志
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/logs `
  -Headers @{ Authorization = "Bearer $token" } -ContentType application/json `
  -Body ("{`"logDate`":`"{0}`",`"content`":`"完成接口联调`"}" -f (Get-Date -Format yyyy-MM-dd))

# 查自己的日志
Invoke-RestMethod -Uri http://localhost:8080/api/v1/logs `
  -Headers @{ Authorization = "Bearer $token" }

# 上级查看下属（把 staffUserId 换成员工 id）
$leader = Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/v1/auth/login `
  -ContentType application/json -Body '{"username":"leader","password":"demo1234"}'
Invoke-RestMethod -Uri "http://localhost:8080/api/v1/logs?userId=$($login.userId)" `
  -Headers @{ Authorization = "Bearer $($leader.token)" }
```

## Docker Compose（MySQL + API）

在仓库根目录：

```bash
docker compose up -d --build
```

API：`http://localhost:8080/api/v1`。配置见根目录 `.env.example`。

## CI / CD（OpenSpec change `github-actions-ci`）

- **CI**：`.github/workflows/ci.yml` — PR / push 到 `main` 时跑 `backend` 的 `mvn -B test`（H2，不连云库）
- **Web**：仅当存在 `web/package.json` 时构建；当前 Vue 前端已提供
- **CD**：`.github/workflows/deploy.yml` — 默认跳过；仓库 Variables 设 `DEPLOY_ENABLED=true`，并配置 Secrets `SSH_HOST` / `SSH_USER` / `SSH_KEY`（可选 `DEPLOY_PATH`）后才会 SSH 部署
- **Android**：不进合并 CI 门槛
- **分支保护**：等 Actions 首次跑绿后再勾「必须通过 CI 才能合并」，避免锁死仓库

本地跑与 CI 相同的测试：

```bash
cd backend
mvn -B test
```

## 模块

- `auth`：注册、登录、JWT
- `log`：日志新建/列表/草稿/提交/作者更新、完整性校验占位
- `panel`：四板块读取（授权派发与今日日志已通，公司/个人十大事后续补）
- `task`：派发与权限范围查询、责任人反馈进度；进度决定状态；Flyway V3 建立 dispatch_tasks 表
- `user`：用户实体与 `manager_id` 组织关系
