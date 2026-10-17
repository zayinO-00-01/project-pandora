# Project Pandora · 智能掌上工作系统

企业内部 **工作日志 + 任务分发 + 信息面板 + 数据分析** 系统。

当前安卓验收包：[0.5.7 安装包](deliverables/Pandora-0.5.7-calendar-views-debug.apk)。补充月、周、日视图，去掉刷新与日历日期切换按钮描边；详见[验收说明](docs/月周日视图.md)。


- **Android App**：工作日志、任务接收、月历、四种业务角色与手动浅色/深色模式
- **Web 管理端**：公司事项/派发、向下穿透查看、数据看板
- **Backend API**：账号权限、日志、任务、面板聚合

课程仓库：[github.com/zayinO-00-01/project-pandora](https://github.com/zayinO-00-01/project-pandora)

## 仓库结构

```
android/     # Android 客户端
web/         # Web 管理端
backend/     # 后端 API
openspec/    # OpenSpec 规范驱动开发
docs/        # 需求、设计、协作、周报
```

## 当前可演示版本

仓库根目录双击 `start-demo.cmd`，打开 http://localhost:8080，用 `leader` 或 `admin` / `demo1234` 登录。已完成 Android 与 Web 的草稿、提交、修改和授权查看，以及 Web 任务派发、Android 责任人进度反馈和 Web 刷新查看。Android 安装包在本机 `deliverables/Pandora-MVP-debug.apk`，手机配置电脑局域网地址后用 `staff / demo1234` 登录。

[本地演示与手动检查](docs/本地演示.md) · [任务演示](docs/任务演示.md) · [MVP验收](docs/MVP验收.md) · [Android工程与构建](android/README.md) · [Android 接口说明](docs/Android接口说明.md)

当前 Android 版本为 `0.4.30-ink-herons`（versionCode 33）。日历只保留月历；四种业务角色分别展示个人、团队、部门和公司工作入口。向下查看受组织关系限制，向上只能申请查看直属上级，由被查看者审批；草稿仅本人可见。主题仅有手动浅色/深色，登录背景固定。日志可查看任意历史日期，仅能写入七天前至当天；需要同步升级后端，详见 [日志七天修改规则](docs/日志七天修改规则.md)。AI 暂不实现。

[代码交付与 PR 指引](docs/代码交付与PR.md) · [角色权限与申请](docs/角色权限与申请.md)

## 快速开始

```bash
git clone https://github.com/zayinO-00-01/project-pandora.git
cd project-pandora
```

### 后端（本地 H2，无需 Docker）

```bash
cd backend
mvn spring-boot:run
```

- 健康检查：`GET http://localhost:8080/api/v1/health`
- 演示账号：`staff` / `leader` / `admin`，密码均为 `demo1234`
- 说明见 [backend/README.md](backend/README.md)

### Docker Compose（MySQL + API）

```bash
docker compose up -d --build
```

### CI / CD

- PR / push `main`：GitHub Actions 构建后端测试（见 `.github/workflows/ci.yml`）
- 可选部署：Variables `DEPLOY_ENABLED=true` + SSH Secrets（见 `.github/workflows/deploy.yml`）
- 详情与 OpenSpec change：`openspec/changes/github-actions-ci/`

各子目录 README 见对应文件夹。

## 文档入口

| 文档 | 说明 |
|------|------|
| [docs/协作约定.md](docs/协作约定.md) | Git、PR、飞书看板、OpenSpec、周报 |
| [docs/过程交付清单.md](docs/过程交付清单.md) | 对照课程要求的完成情况 |
| [docs/团队分工.md](docs/团队分工.md) | 五人模块分工与测试责任 |
| [docs/backlog.md](docs/backlog.md) | 用户故事、估算、Sprint |
| [docs/产品需求与设计-初版.md](docs/产品需求与设计-初版.md) | Sprint 0 需求与设计初稿 |
| [openspec/](openspec/) | OpenSpec 规格与变更 |

## 成员与 GitHub

请在 [docs/团队分工.md](docs/团队分工.md) 中填写五人姓名与 GitHub ID（结题考核需要）。

## 许可

课程实训项目，仅供本课程组内使用。
