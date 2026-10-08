## 1. Workflow 骨架

- [x] 1.1 新增 `.github/workflows/ci.yml`：在 PR 与 push 到 `main` 时触发
- [x] 1.2 配置 JDK 17 + Maven 缓存，于 `backend/` 执行构建（优先 `mvn -B test`，必要时保证可绿）
- [x] 1.3 确保 CI 未配置云数据库环境变量，沿用 H2/本地测试库
- [x] 1.4 添加条件 Web 构建步骤：仅当 `web/package.json` 存在时构建（step 内检测文件，避免 job 级 `hashFiles`）

## 2. 后端测试可跑通

- [x] 2.1 如尚无测试，在 `backend/` 增加最小 Spring Boot 测试（如 context load 或 health），使 `mvn test` 有意义
- [x] 2.2 本地或 CI 确认 `mvn -B test` 通过（本地 3 tests BUILD SUCCESS）

## 3. CD 与文档

- [x] 3.1 新增可选 deploy workflow 或在 `ci.yml` 中增加 deploy job：仅当 SSH 相关 Secrets 存在时执行；否则 skip
- [x] 3.2 更新 `backend/README.md` 或根 `README.md`：说明 CI 触发条件、Web 条件构建、CD 需配置的 Secrets 名称、暂不强制 branch protection
- [x] 3.3 确认未将密钥写入仓库；`.env.example` 仍无真实密码

## 4. 验收

- [ ] 4.1 通过 PR 或 workflow 手动验证：Checks 中后端 job 成功；无 Android 必过打包 job（需 push/PR 后在 GitHub Actions 确认）
- [ ] 4.2 勾选本 tasks 完成项，并在 PR 描述关联 change `github-actions-ci`（开 PR 时写明 change 名）
