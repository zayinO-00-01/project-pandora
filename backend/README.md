# Backend API

智能掌上工作系统 · 后端服务（Spring Boot 3 / Java 17）。

契约：[`openapi.yaml`](./openapi.yaml)。当前已实现：登录、ADMIN 用户管理、日志读写与穿透、完整性校验占位、面板读取（今日日志）、健康检查。公开注册已关闭。

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

## 演示账号（密码均为 `demo1234`）

组织链：`staff → team_lead → leader(dept) → founder`；`admin` 仅 Web。

| 用户名 | 角色 | 说明 |
|--------|------|------|
| `admin` | ADMIN | 系统管理员；用户管理 API |
| `founder` | FOUNDER | 创始人；可查业务用户日志 |
| `leader` | DEPT_HEAD | 部门老总；向下子树 |
| `team_lead` | TEAM_LEAD | 团队长；向下子树 |
| `staff` | STAFF | 员工 |

公开 `POST /auth/register` 已关闭；建号用 `POST /api/v1/admin/users`（需 admin token）。

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
- **Web**：仅当存在 `web/package.json` 时构建；当前无前端工程则跳过
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
- `log`：日志新建/列表、完整性校验
- `panel`：四板块读取（公司事项后续补；今日日志已通）
- `user`：用户实体与 `manager_id` 组织关系
