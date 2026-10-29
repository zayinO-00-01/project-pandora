> 最新交付为 0.7.0：协作关键词、固定悬浮操作与总览转场；需要配套 APK 和后端 JAR，见 [升级说明](../docs/协作关键词与总览转场.md)。

# Pandora API

> 0.6.0更新：任务/日志分离、四宫格总览、审核通知、品牌标签、24小时日志编辑及个人账号设置。最新规则见 [任务工作台与四宫格总览](../docs/任务工作台与四宫格总览.md)。


Spring Boot3 / Java17；默认本机 H2 文件库，公开注册关闭。契约见 [openapi.yaml](openapi.yaml)。推荐根目录 `start-demo.cmd` 启动，Web/API 同端口。单独开发在本目录 `mvn spring-boot:run`。健康检查 `GET /api/v1/health`。

ADMIN 仅管账号。FOUNDER 可读公司业务账号，DEPT_HEAD/TEAM_LEAD 按实际下级树，STAFF 仅本人。获批查看直属上级只可读其已提交日志和任务，不授予写入或派发。停用账号无法登录，旧token失效。

## 数据升级

V1 初始化 → V2 演示账号 → V3 五角色及停用 → V4 任务 → V5 查看申请。首次安装及原 main V3 库直接由 Flyway 升级。旧 zayin 使用 V3 tasks，不能仅改迁移文件名或全局repair。

停止服务后在根目录运行：

```powershell
.\scripts\Upgrade-DemoDatabase.ps1
# 不带 .mv.db 后缀，仅支持 H2 / sa / 空密码
.\scripts\Upgrade-DemoDatabase.ps1 -DatabasePath 'D:/demo/data/pandora'
```

工具只接受已知历史与校验值，先生成一致性ZIP，在副本预演并核对日志任务内容；随后独占原库、检查预演期间数据未变，再原地升级。保留升级前 `.mv.db` 备份，不用快照替换原库。未知迁移拒绝转换。默认启动器自动执行；自定义 `SPRING_DATASOURCE_URL` 不自动处理。失败不删数据；恢复时停止服务，保留当前文件，再将备份还原为 `pandora.mv.db`。

账号及步骤见 [本地演示](../docs/本地演示.md)。开发JWT secret和演示密码仅供本地演示，部署时通过环境变量设置。MySQL使用mysql profile；本轮本地演示以H2为验证目标。
