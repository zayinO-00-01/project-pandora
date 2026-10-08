## Why
现有后端可登录和新建日志，但缺日志修改、草稿可见性及可操作的 Web 页面，无法演示员工记录与管理者查看的完整流程。

## What Changes
- 沿用 Spring Boot API，增加日志更新/提交、草稿隐私及管理者可查询的成员列表。
- 增加 Vue Web 工作台：管理者登录、人员/日期筛选、日志详情、刷新和错误提示。
- 本地启动脚本构建并从同一服务提供 Web 与 API；提供员工接口演示脚本、Android 联调说明及手动验收清单。
- 新建日志未传 status 时保持 submitted，兼容已有客户端；Android 不修改。

## Capabilities
### New Capabilities
- `log-demo`: 日志草稿、提交、编辑、授权查看和成员选择。
- `web-log-console`: Web 管理者登录和真实日志查看。
- `local-demo`: 本地一键运行、接口演示及数据持久化。
### Modified Capabilities
无归档规格；本变更补充 sprint-0-baseline 的演示实现，不承担 Android、派发及完整日历。

## Impact
backend 日志、用户查询、异常处理和权限；web 新工程；scripts 启动工具；接口和产品说明。只使用本地 H2/可选 MySQL，不访问云库。
