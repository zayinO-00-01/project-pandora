# Project Pandora · 智能掌上工作系统

企业内部 **工作日志 + 任务分发 + 信息面板 + 数据分析** 系统。

- **Android App**：员工日常日志、任务接收、月/周/日视图
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

当前为日志与任务闭环原型；公司/个人十大事、请示、完整日历、月相主题等仍待后续，AI 暂不实现。

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
