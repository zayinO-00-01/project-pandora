# Project Pandora · 智能掌上工作系统

企业内部 **工作日志 + 任务分发 + 信息面板 + 数据分析** 系统。

**当前开发入口：[首轮 Web MVP 开发文档](docs/mvp/README.md)。** 2026-09-24 调整为一周内优先交付手机/电脑浏览器可用的“登录 → 写日志 → 真实保存 → 直属上级查询”流程。当前仍为设计阶段，业务工程尚未实现。Android 仍属最终课程交付，本轮暂缓。

- **Android App**：员工日常日志、任务接收、月/周/日视图
- **Web**：首轮同时提供员工日志和领导查询入口；后续再扩展公司事项/派发和数据看板
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

## 快速开始

> 当前尚无可启动业务工程。按首轮 MVP 计划尽早建立可运行增量，不等待完整产品所有模块设计完毕；具体启动命令在工程创建并验证后补充。

```bash
git clone https://github.com/zayinO-00-01/project-pandora.git
cd project-pandora
```

各子目录 README 见对应文件夹。

## 文档入口

| 文档 | 说明 |
|------|------|
| [docs/mvp/README.md](docs/mvp/README.md) | 当前首轮范围、技术架构、接口端口、数据模型、一周计划、腾讯云部署 |
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
