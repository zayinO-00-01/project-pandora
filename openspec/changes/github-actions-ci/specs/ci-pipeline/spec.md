## ADDED Requirements

### Requirement: PR 构建验证后端

系统 SHALL 在指向 `main` 的 Pull Request 上通过 GitHub Actions 构建后端工程，且测试或构建过程不得连接云端 MySQL 或其他生产数据库。

#### Scenario: Pull Request 触发后端构建

- **WHEN** 开发者向 `main` 发起或更新 Pull Request
- **THEN** GitHub Actions 运行后端 Maven 构建（至少 `package` 或 `test`），使用本地/H2/CI 内数据库配置，不读取云数据库连接

#### Scenario: 后端构建失败阻止通过检查

- **WHEN** 后端编译或测试失败
- **THEN** 该 workflow job 以失败结束，在 PR Checks 中可见失败状态

### Requirement: Web 构建条件执行

当仓库存在可构建的 Web 前端工程时，系统 SHALL 在同一 PR workflow 中构建 Web；若尚无 Vite/Node 工程，则 SHALL 跳过 Web 构建步骤且不因此失败。

#### Scenario: 尚无 Web 源码时跳过

- **WHEN** PR 触发 CI 且 `web/` 下不存在可识别的前端工程清单（如 `package.json`）
- **THEN** Web 构建步骤被跳过，整体 CI 不因缺失 Web 而失败

#### Scenario: 存在 Web 工程时构建

- **WHEN** PR 触发 CI 且 `web/package.json` 存在
- **THEN** Actions 安装依赖并执行生产构建（如 `npm ci` + `npm run build`）

### Requirement: Android 不作为合并 CI 门槛

系统 SHALL NOT 将 Android 应用打包作为 Pull Request 合并所需的必过检查。

#### Scenario: PR 无 Android 必过 job

- **WHEN** 查看指向 `main` 的 PR Checks
- **THEN** 不存在「必须成功才能合并」的 Android 打包 job（本 change 不添加该类 required check）

### Requirement: CD 默认安全跳过

合并进 `main` 后的远程部署 SHALL 仅在配置了必要仓库 Secrets 时执行；未配置时 SHALL 跳过部署且不导致流水线硬失败（或使用独立、默认可跳过的 deploy job）。

#### Scenario: 未配置 SSH Secrets

- **WHEN** 代码推送到 `main` 且未配置部署所需 Secrets
- **THEN** 部署步骤跳过或 deploy job 标记为 skipped，不影响仓库默认可用性

### Requirement: 密钥不进仓库

CI/CD 使用的密钥与数据库密码 SHALL NOT 提交进 Git；仓库仅保留 `.env.example` 一类无密钥模板。

#### Scenario: Workflow 使用 Secrets

- **WHEN** 未来启用 SSH 部署
- **THEN** 凭证仅来自 GitHub Actions Secrets，不出现在 workflow 明文或已跟踪源文件中
