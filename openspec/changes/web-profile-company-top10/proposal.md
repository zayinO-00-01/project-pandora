## Why
Web 账号仅有账号名、显示名、角色与上下级，缺少手机号、身份证号、出生年月等人事信息，管理员无法在账号管理中维护完整档案。公司十大重要事项尚未实现发布入口，面板的公司事项板块仍为空。
## What Changes
- 用户档案富化：新增手机号（必填唯一）、身份证号（选填唯一）、出生日期、性别、入职日期、紧急联系人及电话、住址等字段。
- ADMIN 在 Web 账号管理中录入与维护；业务用户仅可查看本人档案，不可改他人；身份证号等敏感字段仅本人与 ADMIN 可见。
- 公司十大重要事项：ADMIN 在 Web 集中发布、编辑、排序与停用，上限 10 条启用事项，全员只读条形展示，可进详情。
- 个人备注：每人可对每条事项写一条个人备注，仅本人可见可改。
- **BREAKING**: `GET /admin/users` 与 `GET /users` 响应新增档案字段；`POST/PATCH /admin/users` 接受新增字段并校验。
## Capabilities
### New Capabilities
- `user-profile`: 用户人事档案字段、校验、可见性与维护规则。
- `company-top10`: 公司十大重要事项的发布、排序、只读展示与个人备注。
### Modified Capabilities
- `user-admin`: 账号管理接口承载新增档案字段。
- `web-log-console`: Web 账号管理页新增档案录入与公司事项维护入口。
- `integrated-workspace`: 面板的公司事项板块接入已发布事项。
## Impact
Spring Boot user 模块与 Flyway V7/V8；Web 账号管理页；面板公司事项板块；OpenAPI、接口说明与验收记录。本地 H2 验证，另保留 MySQL 配置。上下级与任务权限规则不变。
