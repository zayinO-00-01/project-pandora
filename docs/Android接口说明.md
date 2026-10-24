# Android联调接口

前缀 `/api/v1`，带 `Authorization: Bearer <token>`。`POST /auth/login` body `{username,password}`，返回 `{token,userId,role,displayName}`。ADMIN仅Web，另外四角色使用App；LEADER已迁移为DEPT_HEAD，公开注册关闭。

| 操作 | 方法和路径 | 内容 |
|---|---|---|
| 日志 | GET /logs?userId=&date= | 省略userId为本人；本人含草稿，他人仅已提交 |
| 保存 | POST /logs | {logDate,content,status}，省略status默认submitted |
| 修改 | PUT /logs/{id} | 同上，省略status保留原状态，提交后不能回草稿 |
| 提交 | POST /logs/{id}/submit | 同ID，无body |
| 成员 | GET /users | 含本人，仅公开摘要 |
| 组织 | GET /users/organization | {members,manager,assignees}，members不含本人，manager可null；assignees为实际可派发下级 |
| 申请列表 | GET /view-requests | 本人发起和待处理 |
| 发起 | POST /view-requests | {targetId,reason}，仅直属上级 |
| 审批/撤销 | PUT /view-requests/{id} | {status:approved或denied或revoked} |
| 任务 | GET /tasks?userId= | 省略按角色范围，指定成员须有读权限 |
| 派发 | POST /tasks | {title,detail,priority,assigneeId,dueAt} |
| 反馈 | PUT /tasks/{id}/progress | {status,progressNote,progress:null}，仅责任人；新App不收集百分比 |
| 本人档案 | GET /users/me | 本人完整档案，含身份证号等敏感字段 |
| 公司事项 | GET /company-notices | 已启用事项列表，含本人备注 `myNote` |
| 事项详情 | GET /company-notices/{id} | 仅已启用；停用事项 404 |
| 写本人备注 | PUT /company-notices/{id}/note | {content}，最多2000字；每人每条唯一，只返回本人备注 |
| 首页 | GET /panels/map | 本人今日日志、授权任务前10、已发布的公司十大事（含本人备注） |

日志只可写上海today-7至today两端包含的8个日期，历史可读。新建无效日期400，旧记录超期更新/提交403，同时检查原日期和目标日期。正文非空最多5000字；提交后可编辑，保持ID和提交状态。

组织摘要{id,username,displayName,role}。申请获批只读目标已提交日志及任务，不含草稿或写权限；撤销及相关角色/组织/停用变化使旧授权失效。重复待审批/已批准申请409。

档案字段：`phone`（必填且唯一，`1[3-9]\d{9}`）、`idCard`（选填唯一，15位数字或18位末位可为X）、`birthDate`（不晚于今天）、`gender`（`MALE`/`FEMALE`/`UNSPECIFIED`）、`hireDate`、`emergencyContact`、`emergencyPhone`、`address`。格式或唯一性不合规一律 400。身份证号只对本人（`/users/me`）与 ADMIN 可见，`/users` 与组织成员列表不返回该字段。业务端没有任何修改他人档案的入口。

公司十大事：ADMIN 在 Web 端发布、排序、停用，启用上限 10 条（服务端强制，超限 400），排序按 `sortOrder` 再按 `id`。全员只读；每条可写一条本人备注，服务端持久化、只有本人能读回（`myNote`），其他成员看到的是 `null`。ADMIN 自己的备注走管理端 `GET/PUT /admin/company-notices/{id}/note`，因为业务端点对所有 ADMIN 一律 403。

任务title非空最多128字，detail最多5000字；priority为low/normal/high默认normal；dueAt可null或带时区ISO8601。响应{id,title,detail,priority,status,dueAt,progress,progressNote,createdBy,assigneeId,creatorName,assigneeName,createdAt,updatedAt}。status独立保存为todo/doing/done，新任务todo且progress=null。progress仅为旧客户端兼容保留；新App固定传null，界面无百分比或任务进度条。旧请求填值时必须是0–100整数：todo仅0、doing为0–99、done仅100；说明非空最多2000字。新App明确发送status，只有缺少status的旧百分比请求才兼容推导。

400参数错误，401重登，403无权/日志只读，404不存在，409申请重复或状态冲突（重复用户名400）。失败保留输入，不虚报成功。完整契约见 [OpenAPI](../backend/openapi.yaml)，运行见 [本地演示](本地演示.md)。`/logs/integrity`仅兼容旧App；新App不调用它阻塞日志编辑和保存，不代表真实完整性校验。
