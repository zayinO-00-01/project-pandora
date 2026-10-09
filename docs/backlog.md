# Backlog

仓库内需求索引。飞书多维表格（或 CodeArts）是正式看板，本文件与之同步，避免只改一边。

字段：优先级、初估（人日，可在后续 Sprint **重估**）、计划 Sprint、状态、OpenSpec change。

状态：`todo` / `doing` / `done` / `deferred`

| ID | 故事 | 优先级 | 初估 | Sprint | 状态 | OpenSpec |
|----|------|--------|------|--------|------|----------|
| US-A1 | 注册/登录 | P0 | 2 | 1 | todo | sprint-0-baseline / auth |
| US-A2 | 分配角色 | P0 | 1 | 1 | todo | auth |
| US-A4 | 未登录不可访问业务数据 | P0 | 1 | 1 | done | log-demo / android-log-client |
| US-B1 | 新建当日日志 | P0 | 2 | 1 | done | log-demo / android-log-client（独立完整性校验另见US-B5） |
| US-B3 | 按日查看自己的日志 | P0 | 1 | 1 | done | android-log-client |
| US-B5 | 每日首次进日志页完整性校验 | P0 | 1 | 1 | todo | work-log |
| US-D1 | 导图四宫格可见 | P0 | 2 | 1 | todo | panel |
| US-F1 | Web 登录 | P0 | 1 | 1 | done | web-log-mvp |
| US-A3 | 「我的」资料 | P1 | 1 | 2 | todo | auth |
| US-B2 | 作者编辑日志（含已提交） | P0 | 1 | 2 | done | log-demo / android-log-client |
| US-B4 | 上级查看下属日志 | P0 | 2 | 2 | done | log-demo / web-log-console |
| US-C1 | 创建任务并指定责任人 | P0 | 2 | 2 | done | task-dispatch |
| US-C2 | 责任人接收任务 | P0 | 1 | 2 | done | task-dispatch |
| US-C3 | 更新进度 | P0 | 1 | 2 | done | task-dispatch |
| US-C4 | 领导查看完成情况 | P0 | 1 | 2 | done | task-dispatch |
| US-C5 | 禁止越权改任务 | P0 | 1 | 2 | done | task-dispatch |
| US-C6 | 向上级发起邀约请示 | P1 | 2 | 2 | todo | task |
| US-D2 | 管理员编辑公司 10 大事 | P0 | 2 | 2 | todo | panel |
| US-D3 | 派发出现在公司派发面板 | P0 | 1 | 2 | done | task-dispatch（授权范围最新10条） |
| US-D4 | 个人 10 大汇总并可改 | P1 | 2 | 2 | todo | panel |
| US-D5 | 面板备注 | P1 | 1 | 2 | deferred | panel |
| US-E1 | 日视图 | P0 | 2 | 1–2 | doing | android-log-client（按日日志完成，任务关联待做） |
| US-E2 | 周视图 | P1 | 2 | 2 | todo | calendar-view |
| US-E3 | 月视图 | P1 | 2 | 2 | todo | calendar-view |
| US-F2 | Web 维护公司事项 | P0 | 2 | 2 | todo | panel |
| US-F3 | Web 查看团队日志与任务 | P0 | 2 | 2 | done | web-log-console / task-dispatch |
| NFR-1 | 按当天月相切换主题 | P0 | 1 | 2 | todo | theme-nfr |
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
