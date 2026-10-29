# Pandora API

Spring Boot3 / Java17，默认本地 H2。推荐根目录 `start-demo.cmd` 将 Web 和 API 打包到同一服务。单独开发可运行 `mvn spring-boot:run`。健康检查：`GET /api/v1/health`。契约见 [openapi.yaml](openapi.yaml)。

ADMIN 管理账号、档案和公司事项；四个业务角色使用 Android。任务、日志按实际组织授权；个人事项和备注归属取自登录身份。个人事项用 revision 检测跨设备保存冲突。

## 数据升级

当前序列：V1 初始化 → V2 演示账号 → V3 任务 → V4 查看申请 → V5 五角色及停用 → V6 任务状态 → V7 员工资料 → V8 公司事项与备注 → V9 个人事项 → V10 自主任务审批。

旧 zayin 曾使用 V3 五角色、V4 任务、V5 查看申请。不能仅重命名文件后启动。停止服务后运行启动器，或先执行：

```powershell
.\scripts\Upgrade-DemoDatabase.ps1
# 参数不含 .mv.db 后缀；仅支持本地 H2 / sa / 空密码
.\scripts\Upgrade-DemoDatabase.ps1 -DatabasePath 'D:/demo/data/pandora'
```

工具只转换校验值完全匹配的已知历史：先备份，在副本预演并比对业务数据，再独占原库执行。原有业务数据不作为测试记录修改；备份保留在数据库旁。未知迁移失败时停止并保留文件，人工检查；不删库、不全局 repair。

首次安装和已匹配当前序列的库由 Flyway 正常迁移；自定义 `SPRING_DATASOURCE_URL` 不自动执行离线兼容工具。MySQL 使用 mysql profile，本轮验证目标为 H2。默认 JWT 密钥和 demo 密码用于本地演示，部署另设环境变量。

自动测试用独立测试库，真实接口联调用原库副本，避免污染演示记录。

## 自主任务审批

四个业务角色均可向自己创建任务。FOUNDER 自主任务直接通过；其他角色须配置启用、业务角色且职级更高的直属上级，任务初始 `approvalStatus=pending`，未配置有效上级返回400。领导向实际下级派发仍直接通过，ADMIN 不办理业务。

`GET /api/v1/tasks/pending-approvals` 返回登录用户当前可审批的直属下级自主任务。`PUT /api/v1/tasks/{id}/approval` 提交 `{"decision":"approved|rejected","note":"选填，最多500字"}`，返回更新后的任务。仅当前直属上级可审，禁止本人或更高层越级代审；组织调整即时改变审批权。重复审批返回409。

任务响应追加 `approvalStatus`、`approvalNote`、`reviewerId`、`reviewedAt`。未人工审批时后两项为null；审批保留工作状态、进度和反馈原值。pending/rejected 任务仍在授权任务列表中显示，禁止更新进度（409），不进入公司派发面板；面板在数据库过滤后取最新10条。创建、审批、反馈使用同一组织锁，避免组织调整或并发审批绕过权限。V10 将历史任务兼容标记为approved，不重置已有进度。