## 1. 规格与文档同步

- [x] 1.1 确认本 change 产物完整（含 PDF 第 6 页五角色与四级 App 链）
- [x] 1.2 更新产品需求文档：Web 仅 ADMIN；App 四级；演示路径员工 App 写日志 → 上级 App 按穿透规则可见
- [x] 1.3 更新团队分工与页面清单：P5 为管理员后台；取消 Web 看下属日志

## 2. 后端数据与鉴权

- [x] 2.1 角色枚举改为 ADMIN / FOUNDER / DEPT_HEAD / TEAM_LEAD / STAFF；迁移原 LEADER 种子数据
- [x] 2.2 Flyway 增加 `users.disabled`；登录拒绝停用用户
- [x] 2.3 关闭公开自助注册；OpenAPI 同步
- [x] 2.4 实现 ADMIN 用户管理 API（创建/改角色/设 manager_id/停用）；非 ADMIN → 403
- [x] 2.5 实现日志可见范围：STAFF 本人；TEAM_LEAD/DEPT_HEAD 向下子树；FOUNDER 全公司业务用户
- [x] 2.6 单测覆盖：停用、403、各级穿透与越权

## 3. 契约与联调

- [x] 3.1 更新 `backend/openapi.yaml`（角色枚举、admin users、登录响应）
- [x] 3.2 PR 说明五角色与 Web/App 边界；演示账号含四级链示例
- [x] 3.3 `mvn -B test` 通过后开 PR（关联 `admin-web-app-clients`）
