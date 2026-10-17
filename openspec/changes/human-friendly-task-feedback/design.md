## Context
现有状态由 progress 派生；数据库 progress 非空；Kotlin Int 也不支持未知进度。当前使用本地 H2，另保留 MySQL 配置。保留队友水墨风格和现有数据。
## Goals / Non-Goals
Goals: 仅用状态和说明反馈、原生选择时间、清晰文案、可安装APK。
Non-Goals: 公司事项发布、个人重点持久化、请示审批、云端部署。
## Decisions
- status 新列独立存储，V6按原0/1..99/100回填，progress放开NULL。新建任务todo且无百分比。
- 新App仅收集status和必填说明，progress固定null；所有页面不显示百分比或任务进度条。服务端暂留旧客户端百分比校验与兼容，不改历史数据。
- 仅旧请求缺少status且带整数progress时推导状态，保障旧APK暂时能写入。新App总发送明确状态。
- 旧本机反馈草稿缺少status时只在读取草稿时按旧百分比恢复状态，随后清空旧百分比并保留说明；新草稿保留明确状态和说明。
- 采用系统DatePickerDialog及TimePickerDialog，按上海日期时间转UTC；取消不更改输入，可清除截止时间。
- 日志直接编辑和保存，实际业务接口承担鉴权，不再调用固定成功校验；保留旧接口以兼容旧APK。
- 周视图复用periodDates与现有记录；未设截止任务仍在任务页查看，不伪造日程。
## Risks / Trade-offs
旧App无法准确显示未知百分比 → 提示组员更新APK；旧写入仅作过渡兼容。
DDL失败不能依赖事务回滚 → 先用旧结构迁移测试；升级实际数据前停止服务并备份，完成后核对原记录。
## Migration Plan
先通过API与旧结构迁移用例，再构建；短暂停止本地演示，备份数据库，启动Flyway V6并检查日志/任务保留。遇失败保留原库与备份，不删数据、不repair历史。
## Open Questions
公司事项维护角色由用户与老师讨论，本次不涉及。
