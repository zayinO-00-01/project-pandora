## Context

产品文档 §3.1 已选定 GitHub Actions。仓库现有 Issue/PR 模板，但无 `.github/workflows/`。`backend/` 已是可 `mvn package` 的 Spring Boot 工程（默认 H2）。`web/`、`android/` 仍基本为占位。服务器与 SSH Secrets 尚未就绪。

## Goals / Non-Goals

**Goals:**

- PR → `main`：自动构建后端，证明基线可编译
- Web：有工程则构建，无则跳过
- CD：可文档化/骨架化，无 Secrets 时安全跳过
- 与产品约定一致：CI 不连云库；Android 不进合并门槛；暂不打开「必须 CI 才能合并」

**Non-Goals:**

- 接通真实云主机部署
- Android 打包 CI
- 强制 branch protection required checks（等首次跑绿后再议）
- 引入第二套 CI 系统

## Decisions

1. **Workflow 文件**：`.github/workflows/ci.yml` 处理 PR 与 push 到 `main` 的构建；可选 `.github/workflows/deploy.yml` 仅在 `main` + Secrets 齐全时部署，否则 `if: false` 或 secrets 判断跳过。
2. **后端 Job**：`ubuntu-latest` + JDK 17 + 缓存 Maven；工作目录 `backend/`；命令优先 `mvn -B test`，若无测试也可 `mvn -B -DskipTests package`——实现时补一个最小测试使 `test` 有意义更佳。
3. **数据库**：CI 不设置云库环境变量；沿用应用默认 H2 或 test profile，满足「CI 不连云数据库」。
4. **Web Job**：`if: hashFiles('web/package.json') != ''` 再 `npm ci && npm run build`。
5. **分支保护**：本 change 只加 workflow，不通过 API 改 GitHub 仓库规则；文档提醒 Owner 跑绿后再勾。

## Risks / Trade-offs

- 仅 `package` 无测试时，CI 对逻辑回归保护弱 → 应用阶段补最小测试
- 条件跳过 Web 可能让同学误以为前端已在 CI 覆盖 → README/任务写明条件
- Deploy 骨架若写错 Secrets 名会导致困惑 → 用明确 `if` 与注释
