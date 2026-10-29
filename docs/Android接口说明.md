# Android 联调接口

前缀 `/api/v1`，带 `Authorization: Bearer <token>`。ADMIN 使用 Web，FOUNDER、DEPT_HEAD、TEAM_LEAD、STAFF 使用 App。

| 操作 | 方法和路径 | 内容 |
|---|---|---|
| 登录 | POST /auth/login | {username,password} → {token,userId,role,displayName} |
| 本人资料 | GET /users/me | 本人完整资料，未填写字段可为 null；App 只读 |
| 日志列表 | GET /logs?userId=&date= | 默认本人，含自己的草稿；他人仅已提交 |
| 新日志 | POST /logs | {logDate,content,status}，省略 status 默认 submitted |
| 修改日志 | PUT /logs/{id} | 同上，省略 status 保持原状态，提交后不能改回草稿 |
| 提交草稿 | POST /logs/{id}/submit | 无 body，保持原 id |
| 首页 | GET /panels/map | companyImportant、companyDispatch、personalTop、todayLogs；事项包含 detail |
| 公司原文 | GET /company-notices、GET /company-notices/{id} | {id,title,content,sortOrder,enabled,updatedAt,myNote} |
| 我的备注 | PUT /company-notices/{id}/note | {content}，最多2000字，空字符串清空，仅本人可见 |
| 个人事项 | GET /priorities/personal | {revision,items:[{id,title,detail,sortOrder,myRemark}]} |
| 保存个人事项 | PUT /priorities/personal | {revision,items}，按数组顺序保存，空列表清空；最多10条，title120字、detail2000字 |
| 组织 | GET /users/organization | {members,manager,assignees}，按实际组织授权 |
| 查看申请 | GET/POST /view-requests | POST {targetId,reason}，仅直属上级 |
| 审批／撤销 | PUT /view-requests/{id} | {status:approved或denied或revoked} |
| 任务 | GET /tasks?userId= | 按角色和授权范围读取 |
| 派发 | POST /tasks | {title,detail,priority,assigneeId,dueAt} |
| 状态反馈 | PUT /tasks/{id}/progress | {status,progressNote,progress:null}，仅责任人 |

个人事项新建使用缺省或负 id，服务端分配稳定正 id，不能借用别人的 id。revision 为最近读取的版本，过期返回409；客户端保留草稿，让用户选择服务器内容或明确用本机内容覆盖。公司备注使用 /company-notices/{id}/note，不使用历史设想的 /priorities/company/{id}/remark。

日志按上海日期：今天及前三天可新建，只有今天可修改及提交。历史补录直接提交后只读，不能改日期绕过；历史查询不限日期。正文非空最多5000字。createdAt、updatedAt 是服务端时间，App 显示到秒；没有独立 submittedAt。

任务状态为 todo/doing/done，说明非空最多2000字。新 App 不收集百分比，progress 只兼容旧客户端。dueAt 可空或带时区 ISO8601，未设截止时间不伪造日程。

400参数错误，401需重登，403无权或日志只读，404不存在，409版本或申请冲突。失败保留输入，不虚报成功。完整契约见 [OpenAPI](../backend/openapi.yaml)。
