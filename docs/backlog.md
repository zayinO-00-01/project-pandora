# Backlog

截至2026-10-23的状态以 [当前验收](MVP验收.md) 和本文件为准；下方旧日期段落是历史记录。

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
| US-D1 | 导图四宫格可见 | P0 | 2 | 1 | todo | panel |
| US-F1 | Web 登录 | P0 | 1 | 1 | done | web-log-mvp |
| US-A3 | 「我的」资料 | P1 | 1 | 2 | todo | auth |
| US-B2 | 作者编辑日志（含已提交） | P0 | 1 | 2 | done | log-demo / android-log-client |
| US-B4 | 上级查看下属日志 | P0 | 2 | 2 | done | log-demo / web-log-console |
| US-C1 | 创建任务并指定责任人 | P0 | 2 | 2 | done | task-dispatch |
| US-C2 | 责任人接收任务 | P0 | 1 | 2 | done | task-dispatch |
| US-C3 | 状态反馈与进度说明 | P0 | 1 | 2 | done | human-friendly-task-feedback |
| US-C4 | 领导查看完成情况 | P0 | 1 | 2 | done | task-dispatch |
| US-C5 | 禁止越权改任务 | P0 | 1 | 2 | done | task-dispatch |
| US-C6 | 向上级发起邀约请示 | P1 | 2 | 2 | todo | task |
| US-D2 | 公司 10 大事发布与维护（ADMIN 集中发布/排序/停用，上限 10 条） | P0 | 2 | 2 | done | web-profile-company-top10 |
| US-D3 | 派发出现在公司派发面板 | P0 | 1 | 2 | done | task-dispatch（授权范围最新10条） |
| US-D4 | 个人 10 大汇总并可改 | P1 | 2 | 2 | todo | panel |
| US-D5 | 面板备注（公司事项个人备注已上线；派发面板备注待做） | P1 | 1 | 2 | doing | web-profile-company-top10 |
| US-E1 | 日视图（日志及到期任务） | P0 | 2 | 1–2 | done | human-friendly-task-feedback |
| US-E2 | 周视图（整周日志及到期任务） | P1 | 2 | 2 | done | human-friendly-task-feedback |
| US-E3 | 月历标记及选中日期内容 | P1 | 2 | 2 | done | human-friendly-task-feedback |
| US-F2 | 公司事项维护入口（Web 仅 ADMIN 集中维护，业务转 App 只读） | P0 | 2 | 2 | done | web-profile-company-top10 |
| US-F3 | Web 仅ADMIN账号管理；业务转App | P0 | 2 | 2 | done | integrate-five-role-clients |
| NFR-2 | 鉴权与越权拒绝 | P0 | 1 | 1–3 | doing | log-demo（日志完成，新增业务逐步覆盖） |
| US-H1 | 日志关键词 | P2 | 3 | 3+ | deferred | — |
| US-H2 | MBTI 建议 | P2 | 3 | 3+ | deferred | — |

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

## 2026-10-23 档案与公司事项收尾

`web-profile-company-top10` 已合并并归档，主规格新增 `openspec/specs/user-profile`、`openspec/specs/company-top10`。

- 档案富化：手机号建号必填且唯一（既有演示账号可后续补录），身份证号选填唯一，出生日期/性别/入职日期/紧急联系人/住址可选；本人 `GET /users/me` 可见完整档案，成员/组织列表不暴露身份证号。
- 公司十大事项：ADMIN 在 Web 集中发布/编辑/排序/停用，启用上限 10 条，超限 400；业务用户只读列表与详情，可写个人备注（每人每条唯一，仅本人可见）；面板 `companyImportant` 已接入。
- 迁移：V7 档案列、V8 事项与备注表；本地文件库实测迁移至 V8，V1–V2 既有数据保留，无 checksum 冲突。
- 验证：后端 `mvn -B test` 49 项全绿；Web `npm run build` 通过；CI（Backend/Web）通过。
- 待办：US-D5 派发面板备注、US-D1 导图四宫格、US-D4 个人十大、US-C6 向上邀约请示、US-A3「我的」资料页；Android“公司重点/优先级”与后端事项需对齐数据源；PR #14 待清理。
