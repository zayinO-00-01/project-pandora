## Context
users 表仅有账号、角色、上下级与停用标志；面板 companyImportant 恒为空。Web 仅 ADMIN 可用，App 四业务角色。本次本地 H2 验证，保留 MySQL 配置。上下级、任务与查看申请规则不变。
## Goals / Non-Goals
Goals: 档案字段可录入、可校验、可按角色可见；公司事项可发布、可排序、上限 10 条只读展示、可进详情；个人备注仅本人可见。
Non-Goals: 个人十大工作、请示审批、公司事项多级审核、附件上传、云端部署。
## Decisions
- users 表新增：phone、id_card、birth_date、gender、hire_date、emergency_contact、emergency_phone、address。DB 列均可空但 phone/id_card 建唯一索引（多 NULL 允许，避免伪造既有演示账号手机号）；API 在建号时要求 phone 必填，编辑时提供则校验格式与唯一。
- 手机号格式：`1[3-9]\d{9}`；身份证号：15 位数字或 18 位（前 17 位数字，末位数字/X/x）；出生日期不晚于今天；性别仅限 `MALE/FEMALE/UNSPECIFIED`。
- 建号时手机号必填；ADMIN 可改全部档案字段；本人通过 `GET /users/me` 查看本人档案（含敏感字段），`GET /users` 与组织成员列表默认脱敏（隐藏身份证号，仅本人与 ADMIN 可见完整）。
- 公司事项新表 `company_notices`：title、content、sort_order、enabled、publisher_id、created/updated；启用事项上限 10 条，排序按 sort_order 再按 id。
- 仅 ADMIN 可 POST/PATCH 发布与排序；全员 GET 只读列表与详情；个人备注新表 `company_notice_notes`（notice_id + user_id 唯一），本人读写。
- Web 账号管理页分“账号/档案”两段录入；公司事项独立维护页，条形展示、进详情、可写个人备注。
## Risks / Trade-offs
身份证号明文存储仅用于课程演示 → 文档注明生产需加密或脱敏存储。
启用上限在服务端强制 → 并发发布超限返回 400，不依赖前端限制。
## Migration Plan
Flyway V7 加 users 档案列（含 phone 唯一约束，既有演示账号回填占位手机号）；V8 建公司事项与备注表。先跑 `mvn -B test`，再在副本库验证迁移。
## Open Questions
甲方是否需要导出档案或事项？本次不做，先确认展示与备注即可。
