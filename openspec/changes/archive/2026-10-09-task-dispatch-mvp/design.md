## Context
用户于 2026-10-09 选择“先做任务派发和进度闭环”。这是日志 MVP 后的下一增量，完整甲方范围仍保留。
## Decisions and API contract
继续 Spring Boot/JPA/Flyway、Vue、Kotlin/Compose。一个任务一个稳定 ID，时间采用 ISO8601 Instant，界面按 Asia/Shanghai 展示，日期选择转成当日 23:59:59+08:00。
- GET /api/v1/tasks：按创建时间倒序返回授权任务数组。
- POST /api/v1/tasks：{title, detail, priority, assigneeId, dueAt}；201 返回 Task。title 去空白后非空且最多128字；detail 最多5000；priority 仅 low/normal/high，默认normal；dueAt 可空，不限制过去时间（可登记已逾期事项）。
- PUT /api/v1/tasks/{id}/progress：{progress, progressNote}；200 返回 Task。progress 整数0..100，progressNote 非空且最多2000；0→todo，1..99→doing，100→done；允许责任人纠正已填进度。
- Task：{id,title,detail,priority,status,dueAt,progress,progressNote,createdBy,assigneeId,creatorName,assigneeName,createdAt,updatedAt}。name取displayName或username。新建 progress=0/status=todo/progressNote=""。
- ADMIN 可派给其他非管理员；LEADER 仅派给直属 STAFF；STAFF 不可派发。GET：ADMIN全部；LEADER自己创建/负责及直属成员负责的任务；STAFF本人负责的任务。只有当前责任人能更新进度，管理员/创建者没有代改权限。用户不存在404，无权限403，无登录401，输入错误400。
- /panels/map 的 companyDispatch 取同样授权列表最新10条映射为 {id,title,sortOrder,myRemark:null}；Android 点击进入任务列表/详情。其他两块仍为空。
## Components
backend task包分实体/仓库/服务/请求响应/控制器，V3建表；Web TaskBoard.vue拥有刷新、新建与详情，App仅新增入口；Android TaskScreen.kt与现有ViewModel/ApiClient接入。新页面失败保留输入，保存中禁止重复点击，401退出到登录且不把旧请求写入新会话。
## Validation
后端关键测试先失败再实现：真实派发→员工读取/进度→领导读取、范围权限和无效输入不改原记录、派发面板。Android HTTP契约测试。各自成功后不重复整套测试，最后一次真实后端+Web链路，APK编译签名；Android触摸验收留用户。
## Non-goals
请示邀约、改任务基本信息/转派/删除、任务与日志关联、任务周月聚合、图表、推送和云部署待后续。
