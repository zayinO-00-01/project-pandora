# Backend API

智能掌上工作系统 · 后端服务（Spring Boot 3 / Java 17）。

契约：[`openapi.yaml`](./openapi.yaml)。当前已实现：登录、ADMIN 用户管理、日志读写与穿透、轻量完整性校验（认证、角色及当日日志读取）、面板读取（今日日志）、健康检查。公开注册已关闭。

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

- `auth`：登录、JWT（公开注册关闭）
- `log`：日志新建/列表、完整性校验
- `panel`：四板块读取（公司事项后续补；今日日志已通）
- `user`：用户实体与 `manager_id` 组织关系

## Sprint 1 后端收尾与验收（2026-10-15）

关联 OpenSpec：`sprint-0-baseline` 的 `3A.1–3A.5`，角色规则沿用 `admin-web-app-clients`。

### 已固定的接口边界

- ADMIN 仅用 Web 用户管理，不能 GET/POST 日志、访问日志完整性校验或 App 个人面板；四级 App 角色均不能调用用户管理 GET/POST/PATCH。权限在过滤器中校验，先于请求正文验证。
- 未登录、无效/过期 token、停用用户的旧 token 返回 JSON 401；角色越权返回 JSON 403。请求格式、日期、角色或必填项错误返回 JSON 400。
- STAFF 仅看自己；TEAM_LEAD/DEPT_HEAD 按 `manager_id` 子树读取；FOUNDER 可查业务用户，不可查 ADMIN。
- 建号时用户名 trim 后判重；拒绝自引用、环路、不存在上级、ADMIN 参与业务上下级。变为 ADMIN 前先解除上级及下属；校验失败不保存部分修改。
- 日志维持同日多条、创建即 `submitted`，正文 trim，作者来自认证用户。未增加编辑/草稿接口，不冒充完成整个一期日志功能。
- 完整性接口会实际加载当日日志；空列表可通过，数据库读取失败不会返回固定成功。每日首次调用、失败禁用编辑、保存失败保留输入仍由 App 实现。
- 演示组织首次补建后不再覆盖既有角色、显示名、上级、停用状态。停用立即拒绝旧 token；角色变更对旧 token 按数据库当前角色生效。修改密码不会自动吊销已签发 token。

### 鉴权检查清单与自动化证据

执行 `mvn -B test`，或只执行收尾测试 `mvn -B "-Dtest=Sprint1ApiTests,RestartPersistenceTests" test`。

- [x] 五角色登录；错误口令、未知账号无 token；无效、过期、错误签名 token 返回 401。
- [x] 受保护接口无 token 返回 401；四级业务用户的用户管理操作返回 403；ADMIN 的 App 日志读写、完整性、面板操作返回 403。
- [x] 新建四级组织并完整走通「ADMIN 建号 → STAFF 登录写日志 → 三类上级读取 → 停用后拒绝登录」。该路径验收文案：晨星冒烟测试校验通过。
- [x] 组织内允许、组织外拒绝；本人日志隔离、日期过滤、倒序、不可伪造作者；空正文/日期/JSON 参数错误拒绝且不写入。
- [x] 禁止组织环路、管理员参与业务组织；失败更新不修改角色、显示名、上级或停用状态；可解除上级。
- [x] 停用后旧 token 拒绝访问，历史日志保留；启用后恢复登录；角色调整对旧 token 生效。
- [x] 独立临时文件 H2：启动应用、经 API 写日志与修改用户配置、关闭应用及数据库连接池、重新启动并查询。日志 ID/内容、用户角色/上级/名称/停用/密码均保留，上级仍能读取。

实际结果：`PandoraApplicationTests` 13 项、`Sprint1ApiTests` 45 项、`RestartPersistenceTests` 1 项；合计 **59 项，失败 0，错误 0，跳过 0，BUILD SUCCESS**。本机使用 IntelliJ 内置 Java 21/Maven，编译目标保持 Java 17；未改构建依赖。重启测试是同一 JVM 内关闭并重建两个完整 Spring 应用上下文，不等同于操作系统进程崩溃恢复验证。报告由 Maven 生成在 `backend/target/surefire-reports/`，不提交构建产物。

如果当前 PowerShell 找不到 `mvn`，且本机保留该 IDE 安装，可在 `backend` 目录执行：

```powershell
$env:JAVA_HOME = 'C:\Program Files\JetBrains\IntelliJ IDEA 2025.1.3\jbr'
& 'C:\Program Files\JetBrains\IntelliJ IDEA 2025.1.3\plugins\maven\lib\maven3\bin\mvn.cmd' -B test
```

### 尚需团队/环境验收

- [ ] Android 与 Web 实际页面联调（不能以 MockMvc 接口测试代替）。
- [ ] Docker/MySQL 环境的迁移、保存与重启验证；本轮仅自动验证隔离 H2，不连接云库或现有业务库。
- [ ] App 每日首次完整性校验、失败禁用编辑及保存失败保留输入的界面证据。
- [ ] 将真实联调冒烟响应/截图附到 PR 或 Issue；本轮未代为提交、推送或创建 PR/Issue。

任务 API、公司事项及其余面板数据留在 Sprint 2。本次后端收尾不意味着整个团队 Sprint 1 或全部一期功能均已验收。
