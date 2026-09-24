## ADDED Requirements

### Requirement: Preseeded account authentication
系统 SHALL 使用预置账号登录、BCrypt 密码哈希和有效期 7200 秒的 JWT；首轮不开放注册或角色分配接口。

#### Scenario: Correct credentials
- **WHEN** 预置用户提供正确口令
- **THEN** 返回 token、有效期与不含密码哈希的本人资料

#### Scenario: Invalid credentials
- **WHEN** 用户名或口令错误
- **THEN** 返回 401，不发 token，不区分账号不存在还是口令错误

#### Scenario: Invalid access token
- **WHEN** 无 token、签名无效或已过期的 token 访问业务 API
- **THEN** 返回 401，页面引导重新登录

### Requirement: Server-side current scope
系统 SHALL 根据数据库当前用户角色和 manager_id 检查权限，不依赖客户端筛选或旧 token 角色。

#### Scenario: List direct subordinates
- **WHEN** LEADER 请求直属下属列表
- **THEN** 仅返回 manager_id 为该领导的用户，无下属返回空数组

#### Scenario: Non-leader requests subordinate list
- **WHEN** STAFF 或 ADMIN 请求下属列表
- **THEN** 返回 403
