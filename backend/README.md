# Pandora API

Spring Boot3 / Java17，默认H2文件库。根目录start-demo.cmd将Web和API运行在同一服务。完整契约见[OpenAPI](openapi.yaml)。当前整合目标0.8.0，尚未替换运行中的云端0.7.0。

- ADMIN管理账号、档案、公司事项和自己的私密备注；业务成员通过Android处理工作。
- 自建任务：GET /tasks/pending-approvals、PUT /tasks/{id}/approval；非创始人由当前直属上级批准。领导派发限定实际下级。
- 完成/变更/删除：GET /tasks/reviews、POST /tasks/{id}/approve或reject；保留任务审计与通知。approvalStatus与reviewStatus分别表示创建批准和后续审核。
- 个人清单GET/PUT /priorities/personal使用revision；公司事项及备注使用/company-notices和/admin/company-notices。
- 日志支持endDate和labelId，今天及前三天可创建，仅单独今天可修改；旧单日记录和旧客户端字段兼容。
- 关键词、品牌标签、个人头像和密码设置由本轮PR补充，具体路径见契约；通知为站内刷新读取，不是系统离线推送。

## 数据与构建

V1–V10保持云端原脚本；所有新迁移顺延，先在数据库副本验证，保留全部原字段及审核时间。不得清空原库、覆盖远端数据库或批量修复未知迁移历史。

```powershell
# 从仓库根目录执行，或用本机Maven路径代替mvn
mvn -f backend/pom.xml test
mvn -f backend/pom.xml package -DskipTests
```

旧迁移顺序兼容检查仍由scripts/Upgrade-DemoDatabase.ps1执行。新装会创建独立本地库，不会自动连接组长云端。测试、构建和真实接口结果以当前验收记录为准。
