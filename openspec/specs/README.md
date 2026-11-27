# Current specs

一期规格以 `main` 的 `2fa2104`（2026-11-27，Android `0.10.25` / `88`）为核对基线。规格描述这版源码中的行为，不代表已重新跑测试、验收真机或核实服务器部署版本。

| 要找什么 | 规格 |
|---|---|
| 登录、禁用、层级与授权 | [认证](auth/spec.md)、[客户端与授权整合](integrated-workspace/spec.md) |
| 任务派发、多人责任、审批和标签 | [任务](task-dispatch/spec.md) |
| 日志日期、编辑窗口与可见性 | [日志](log-demo/spec.md)、[Android 日志](android-log-client/spec.md) |
| 首页、公司日历、个人事项与通知 | [工作台](workspace-planning/spec.md)、[交互](human-friendly-workspace/spec.md) |
| 账号档案、部门及通讯录 | [人员档案](user-profile/spec.md)、[账号](user-admin/spec.md)、[通讯录](organization-directory/spec.md)、[Web 管理台](web-log-console/spec.md) |
| 公司十大事项 | [公司事项](company-top10/spec.md) |
| 安装、演示与迁移 | [本地演示](local-demo/spec.md) |

核对入口：基线中的 `backend/openapi.yaml`、`docs/本周功能验收-2026-11-26.md`（0.10.17 历史验证）、`docs/部门与通讯录验收.md`，以及 `SharedTaskTests`、`LogWriteWindowTests`、`PersonalTodoTests`、`DirectoryDepartmentTests` 和档案相关测试。旧版本测试报告不替代 0.10.25 的运行验证。

有两处旧材料与当前代码容易混淆：新填或更改的紧急联系电话只接受手机号，旧座机未改时可保留；日志超出 24 小时后 Android 隐藏标签编辑，但独立标签接口仍允许作者修改标签。规格分别记录界面与接口行为。

二期客户、会员权益、合同和营收属于后续变更，不计入本目录的一期规格。
