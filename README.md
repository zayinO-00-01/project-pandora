# Project Pandora · 智能掌上工作系统

当前原型：五角色账号管理、工作日志、组织查看、直属上级查看申请、任务派发、状态反馈及月/周/日视图。Android 办理业务，Web 仅用于 ADMIN 管理账号。

## 本地运行

Windows 在根目录运行 `start-demo.cmd`。需要 Java17+、Node18+、Maven，首次构建联网。浏览器打开 http://localhost:8080，用 `admin / demo1234`。Android 安装本机构建的 `deliverables/Pandora-MVP-debug.apk`，服务地址填电脑局域网 IPv4 和相同端口。

| 账号 | 角色 | 使用端 |
|---|---|---|
| admin | ADMIN 系统管理员 | Web |
| founder | FOUNDER 创始人 | Android |
| leader | DEPT_HEAD 部门老总 | Android |
| team_lead | TEAM_LEAD 团队长 | Android |
| staff | STAFF 员工 | Android |

初始密码均 `demo1234`，组织为 `staff → team_lead → leader → founder`，管理员后续修改会保留。公开注册关闭。

数据在本机 `backend/data/`。别人下载源码后连接自己的本地服务和数据库。不要删库解决升级问题；启动脚本会备份转换已知旧 zayin H2 库，main 库和首次安装由 Flyway 正常迁移。

[启动与演示](docs/本地演示.md) · [任务流程](docs/任务演示.md) · [接口说明](docs/Android接口说明.md) · [当前验收](docs/MVP验收.md) · [Android构建](android/README.md) · [API契约](backend/openapi.yaml)

## 范围与结构

- `android/`：Kotlin / Compose，水墨登录、月周日历和浅深主题。
- `web/`：Vue3，账号列表、新建、角色与上级调整、停用启用。
- `backend/`：Spring Boot3 / Java17，JWT、权限、H2/Flyway。
- `openspec/`：规格与变更；`docs/`：需求和协作材料。

日志可编辑上海日期今天至七天前，共8个日期；历史可读，提交后可继续编辑。上级申请只授予只读权限。Android 领导向实际下级派发，只有责任人能反馈状态和说明；反馈只填工作状态和说明，不提供百分比输入、展示或任务进度条。截止时间通过日期和时间选择器设置。

公司事项发布待与老师确认；个人重点整理、备注与资料编辑、任务请示邀约和云部署留待后续。当前不是甲方全部需求的最终交付。

[源码仓库](https://github.com/zayinO-00-01/project-pandora)。APK、数据库、密钥和缓存不随源码提交。课程过程材料见 [过程交付清单](docs/过程交付清单.md)。
