## ADDED Requirements

### Requirement: Persistent work log creation and querying
系统 SHALL 允许有效用户创建本人日志并分页查询，后端提交成功后才返回成功。字段与边界以 OpenAPI 0.2.0 为准。

#### Scenario: Create and reread
- **WHEN** 员工填写有效日期和 1–2000 码点正文并保存，随后重新登录
- **THEN** 数据仍可从真实数据库读取，作者来自身份，同日允许多条日志

#### Scenario: Reject invalid content or date
- **WHEN** 正文为空白、超长、日期非法或写入未来日期
- **THEN** 返回 400，不创建记录，页面保留输入并提示

#### Scenario: Paginated history
- **WHEN** 用户按可选单日和合法页码查询本人历史
- **THEN** 仅返回范围内记录，按 logDate/createdAt/id 倒序；合法空结果返回 200

### Requirement: Author editing with optimistic concurrency
系统 SHALL 允许作者修改已保存日志，使用原子版本校验；无草稿、提交和归档状态。

#### Scenario: Successful update
- **WHEN** 作者提交有效正文、日期及当前 version
- **THEN** 保存新值并递增 version，授权上级刷新可见

#### Scenario: Stale update
- **WHEN** 作者提交的 version 已过期
- **THEN** 返回 409，不覆盖数据库新值，客户端保留输入

### Requirement: Direct subordinate read access
系统 SHALL 仅允许本人和其当前 LEADER 直属上级读取日志；仅作者可以修改。管理员首轮无全员读取特权。

#### Scenario: Direct leader reads
- **WHEN** 领导选择直属下属和日期查询列表或详情
- **THEN** 返回范围内已保存日志，不需要员工另行提交

#### Scenario: Unauthorized query or edit
- **WHEN** 员工读取他人日志、领导查询非直属人员，或领导修改下属正文
- **THEN** 后端返回 403，不泄露正文，不写入数据

### Requirement: Reliable save feedback
系统 SHALL 在写入失败时保留当前页面输入，禁止误报成功；重启不丢失已确认保存的数据。

#### Scenario: Timeout or service failure
- **WHEN** 保存失败或请求超时
- **THEN** 保留正文；超时提示先查询核对，不自动重试创建

#### Scenario: Restart persistence
- **WHEN** 成功保存后重启 API 和数据库
- **THEN** 同一条记录仍存在，正文未改变
