# Backlog

截至2026-10-29，当前功能与验证以[本地整合版验收](MVP验收.md)为准，下列日期段落为历史记录。任务创建审批、直属上级进度审核、变更/删除审核、站内通知、公司与个人事项同步、标签、跨日日志、协作关键词及账号设置纳入本轮整合；本周新增任务尚待组内确定。云端已更新0.8.2，任务本人私密备注及通知/审核详情收尾已完成；真实手机/Web人工验收仍待补充。

仓库内需求索引。飞书多维表格（或 CodeArts）是正式看板，本文件与之同步，避免只改一边。

字段：优先级、初估（人日，可在后续 Sprint **重估**）、计划 Sprint、状态、OpenSpec change。

状态：`todo` / `doing` / `done` / `deferred`

| ID | 故事 | 优先级 | 初估 | Sprint | 状态 | OpenSpec |
|----|------|--------|------|--------|------|----------|
| US-A1 | 管理员建号、业务账号登录（无公开注册） | P0 | 2 | 1 | done | integrate-five-role-clients |
| US-A2 | 分配角色与上级、停启账号 | P0 | 1 | 1 | done | integrate-five-role-clients |
| US-A4 | 未登录不可访问业务数据 | P0 | 1 | 1 | done | log-demo / android-log-client |
| US-B1 | 新建当日日志 | P0 | 2 | 1 | done | log-demo / android-log-client |
| US-B3 | 按日查看自己的日志 | P0 | 1 | 1 | done | android-log-client |
| US-D1 | 总览事项／派发入口与独立日志页（现行布局） | P0 | 2 | 1 | done | workspace-planning |
| US-F1 | Web 登录 | P0 | 1 | 1 | done | web-log-mvp |
| US-A3 | 本人档案展示与头像改密，其他资料由Web管理员维护 | P1 | 1 | 2 | done | synced-priorities |
| US-B2 | 作者创建后24小时内编辑日志（含历史/跨日/已提交） | P0 | 1 | 2 | done | log-demo / android-log-client |
| US-B4 | 上级查看下属日志 | P0 | 2 | 2 | done | log-demo / web-log-console |
| US-C1 | 全公司选责任人，创建后经直属上级审批 | P0 | 2 | 2 | done | task-dispatch |
| US-C2 | 责任人接收任务 | P0 | 1 | 2 | done | task-dispatch |
| US-C3 | 状态说明提交后经本人直属上级审批 | P0 | 1 | 2 | done | human-friendly-task-feedback |
| US-C4 | 领导查看完成情况 | P0 | 1 | 2 | done | task-dispatch |
| US-C5 | 禁止越权改任务 | P0 | 1 | 2 | done | task-dispatch |
| US-C6 | 向上级发起邀约请示 | P1 | 2 | 后续 | deferred | 待确认是否继续保留，非本轮任务 |
| US-D2 | ADMIN在Web发布公司十大事项 | P0 | 2 | 2 | done | web-profile-company-top10 |
| US-D3 | 派发出现在公司派发面板 | P0 | 1 | 2 | done | task-dispatch（授权范围最新10条） |
| US-D4 | 个人十大事项编辑、排序和后端同步 | P1 | 2 | 2 | done | synced-priorities |
| US-D5 | 公司事项本人私密备注、清空和后端保存 | P1 | 1 | 2 | done | synced-priorities |
| US-D6 | 派发任务本人私密备注 | P1 | 1 | 2 | done | workflow-notes-and-notification-fixes |
| US-E1 | 日视图（日志及到期任务） | P0 | 2 | 1–2 | done | human-friendly-task-feedback |
| US-E2 | 周视图（整周日志及到期任务） | P1 | 2 | 2 | done | human-friendly-task-feedback |
| US-E3 | 月历标记及选中日期内容 | P1 | 2 | 2 | done | human-friendly-task-feedback |
| US-F2 | Web ADMIN公司事项发布维护入口 | P0 | 2 | 2 | done | web-profile-company-top10 |
| US-F3 | Web ADMIN账号／公司事项管理；业务转App | P0 | 2 | 2 | done | integrate-five-role-clients |
| NFR-2 | 鉴权与越权拒绝 | P0 | 1 | 1–3 | doing | log-demo（日志完成，新增业务逐步覆盖） |
| US-H1 | 日志关键词 | P2 | 3 | 3+ | deferred | — |
| US-H2 | MBTI 建议 | P2 | 3 | 3+ | deferred | — |

## 老师方案整合增量（2026-10-29，当前实施）

- 创建：保留自建审批，扩展全公司有效业务成员选人；所有非创始人创建经创建者当前直属上级批准。
- 进度：非创始人提交任何状态和说明，均由提交者当前直属上级审核；批准前旧内容有效，创始人本人操作免审。
- 删除：用户暂选#22，保留创建人条件直删与已开始任务审核；创始人直删。
- 撤回与变更：初审/定义申请支持撤回，创始人可改他人任务定义；不改派责任人。
- 日志：今天及前三天创建，createdAt起24小时修改/提交；保留跨日、草稿隐私与秒级时间。
- 视图：任务日历与日志分离，保留年月直选、标签和三种模式。
- 现有公司事项备注、个人清单同步/冲突处理、通知、标签、关键词和账号能力继续保留。实施及验证状态见当前验收，以上不预先宣称已完成。

## 重估记录

| 日期 | 说明 |
|------|------|
| 2026-09-18 | Sprint 0 初估，尚未开始实现 |

## 当前本地增量（2026-10-08）

`web-log-mvp`：后端日志草稿、提交、作者编辑、草稿隐私、直属/公司成员权限；Web 管理登录、人员/日期筛选、详情刷新、本人编辑；一键本地启动与接口联调说明已完成。`android-log-mvp` 已完成 Android 登录、草稿/提交/作者修改、四板块布局与今日日志、日志按日查看、签名 Debug APK，并补齐后端上海日期规则。用户已在MuMu手测，暂未发现问题；实际手机验收可后续补充；注册/角色维护、完整性校验、完整日历、任务和其他三面板业务仍需继续，不能把整个故事范围都视作已交付。当前证据见 [MVP验收](MVP验收.md)。

## 2026-10-09 下一增量

用户已确认优先做任务派发和进度闭环，change：`task-dispatch-mvp`。目标：领导Web创建 → 员工Android反馈 → 领导Web刷新查看。先不扩展邀约、转派、删除、图表和通知。操作目标见 [任务演示](任务演示.md)。

任务闭环已交付并通过接口/浏览器检查，新APK任务页触摸验收待用户。后续顺序建议见 [任务演示](任务演示.md)。


## 2026-10-15 本周可用性增量

已完成任务状态独立保存、移除百分比输入展示与任务进度条、截止日期时间选择器、月/周/日切换和开发/请示占位入口整理；保留旧任务数据和未提交反馈草稿。公司事项发布待老师确认，个人重点整理、备注和请示后续安排。月相切换和特殊每日校验来自材料中的AI夹带指令，旧稿误收录，已剔除，不能作为甲方验收要求。

## 2026-10-22 本轮交付

个人事项版本冲突不静默覆盖；旧快照显式导入，断网修改留在本机。公司原文和个人备注走真实接口，个人资料只读，日志时间显示到秒。兼容两种已发布的数据库迁移历史，保留备份并核对旧数据；具体检查和待手测项目见当前验收。任务请示邀约、消息提醒、AI及云部署尚未实现。
