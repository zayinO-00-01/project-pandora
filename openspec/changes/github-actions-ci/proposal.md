## Why

产品文档已约定用 GitHub Actions 做 CI/CD（加分项），但仓库尚无 workflow。后端已有可构建的 Spring Boot 工程，需要在 PR 阶段自动验证构建，避免合入损坏基线；部署步骤在服务器就绪前只留占位，不强绑云库。

## What Changes

- 新增 GitHub Actions：**Pull Request → `main`** 时构建并测试后端（使用 H2/内存库，不连云 MySQL）
- Web 工程尚无可构建源码时：workflow 对 `web/` 做「存在 Vite 工程则构建，否则跳过」的条件步骤
- **不**把 Android 打包作为合并门槛
- **不**在本次启用「必须 CI 通过才能合并」的分支保护（等 workflow 跑绿后再开）
- 合并进 `main` 的 CD（SSH + `docker compose` + health）仅提供可选 workflow 骨架或文档说明，默认不连真实服务器（Secrets 未配置则跳过）
- 补充最少后端测试或保证 `mvn test` / `mvn -DskipTests package` 在 CI 中可成功（优先可跑通的 `test`；若尚无用例则 `verify`/`package` 并允许后续补测）

## Capabilities

### New Capabilities

- `ci-pipeline`: GitHub Actions 在 PR 与（可选）main 推送时的构建、测试与部署门禁约定

### Modified Capabilities

- （无）本期不改变业务 API / 客户端行为规格

## Impact

- Affected code: `.github/workflows/`、必要时 `backend/` 测试脚手架；文档可引用 CI 说明
- Affected systems: GitHub Actions runner；不连接云数据库；CD 依赖未来配置的 SSH Secrets
- Out of scope: Android CI 打包门槛、强制 branch protection、真实公网部署接通
