# Backlog

2026-09-19 更新。故事和优先级与[产品文档](产品需求与设计-初版.md) §5 一致，验收标准统一查看该节。

P0 必做，P1 可选，P2 暂不做。初估单位为人日，供排期参考，后续根据实际进展调整。OpenSpec 能力均属于 `sprint-0-baseline`。

## 开发安排

- Sprint 1：登录、写日志、四板块骨架、日视图及 Web 日志查看。
- Sprint 2：任务和请示、公司事项维护、个人十大事、备注、周/月视图及月相主题。
- Sprint 3：联调、测试和修复。

## 用户故事

| ID | Epic / Feature | 用户故事 | 优先级 | 初估（人日） | Sprint | 状态 | OpenSpec 能力 |
|---|---|---|---|---|---|---|---|
| US-A1 | EP-1 / FT-A | 作为员工，我可以注册并登录。 | P0 | 2 | 1 | todo | auth |
| US-A2 | EP-1 / FT-A | 作为管理员，我可以设置角色和直属上级。 | P0 | 2 | 1 | todo | auth |
| US-A3 | EP-1 / FT-A | 作为用户，我可以修改自己的显示名。 | P1 | 1 | 2 | todo | auth |
| US-A4 | EP-1 / FT-A | 作为用户，我的业务数据受到登录保护。 | P0 | 1 | 1 | todo | auth |
| US-B1 | EP-1 / FT-B | 作为员工，我可以保存工作日志草稿。 | P0 | 2 | 1 | todo | work-log |
| US-B2 | EP-1 / FT-B | 作为员工，我可以修改自己的草稿和已提交日志。 | P0 | 2 | 1 | todo | work-log |
| US-B3 | EP-1 / FT-B | 作为员工，我可以按日期查看自己的日志。 | P0 | 1 | 1 | todo | work-log |
| US-B4 | EP-1 / FT-B | 作为上级，我可以查看下属已提交的日志。 | P0 | 2 | 1 | todo | work-log |
| US-B6 | EP-1 / FT-B | 作为员工，我可以提交日志让上级查看。 | P0 | 1 | 1 | todo | work-log |
| US-C1 | EP-2 / FT-C | 作为领导，我可以创建任务并指定责任人。 | P0 | 2 | 2 | todo | task |
| US-C2 | EP-2 / FT-C | 作为责任人，我可以查看收到的任务。 | P0 | 1 | 2 | todo | task |
| US-C3 | EP-2 / FT-C | 作为责任人，我可以更新进度和说明。 | P0 | 2 | 2 | todo | task |
| US-C4 | EP-2 / FT-C | 作为领导，我可以查看任务完成情况。 | P0 | 1 | 2 | todo | task |
| US-C5 | EP-2 / FT-C | 作为任务参与者，我希望指令和进度按权限修改。 | P0 | 1 | 2 | todo | task |
| US-C6 | EP-2 / FT-C | 作为下级，我可以向直属上级发起邀约请示。 | P0 | 2 | 2 | todo | task |
| US-C7 | EP-2 / FT-C | 作为上级，我可以回复并处理邀约。 | P0 | 2 | 2 | todo | task |
| US-D1 | EP-3 / FT-D | 作为用户，我可以在首页看到四板块。 | P0 | 2 | 1 | todo | panel |
| US-D2 | EP-3 / FT-D | 作为管理员，我可以维护公司十大事。 | P0 | 2 | 2 | todo | panel |
| US-D3 | EP-3 / FT-D | 作为用户，我可以在公司派发板块看到相关任务。 | P0 | 1 | 2 | todo | panel |
| US-D4 | EP-3 / FT-D | 作为员工，我可以整理自己的个人十大事。 | P0 | 2 | 2 | todo | panel |
| US-D5 | EP-3 / FT-D | 作为用户，我可以给公司事项和派发写个人备注。 | P0 | 1 | 2 | todo | panel |
| US-E1 | EP-3 / FT-E | 作为员工，我可以查看日视图。 | P0 | 2 | 1（日志）/2（任务） | todo | calendar-view |
| US-E2 | EP-3 / FT-E | 作为员工，我可以查看周视图。 | P0 | 2 | 2 | todo | calendar-view |
| US-E3 | EP-3 / FT-E | 作为员工，我可以查看月视图。 | P0 | 2 | 2 | todo | calendar-view |
| US-F1 | EP-1 / FT-F | 作为管理者，我可以用同一账号登录 Web。 | P0 | 1 | 1 | todo | auth |
| US-F2 | EP-3 / FT-F | 作为管理员，我可以在 Web 维护公司事项。 | P0 | 1 | 2 | todo | panel |
| US-F3 | EP-1 / FT-F | 作为管理者，我可以在 Web 查看日志和任务。 | P0 | 3 | 1（日志）/2（任务） | todo | work-log, task |

共 27 条，26 条 P0、1 条 P1。跨 Sprint 的 US-E1 暂按日志 1 人日、任务 1 人日；US-F3 按日志 2 人日、任务 1 人日。共享功能不重复估算，例如 US-F2 只估 Web 页面和联调，维护逻辑计入 US-D2。

## 非功能项

| ID | 要求 | 初估（人日） | Sprint | 状态 | OpenSpec 能力 |
|----|------|--------------|--------|------|---------------|
| NFR-1 | 按当天月相切换主题 | 2 | 2 实现 / 3 验证 | todo | theme-nfr |
| NFR-2 | 未登录 401、越权 403 | 1（专项验证） | 1–3 | todo | auth |
| NFR-3 | 保存失败保留输入、重复操作不重复建记录 | 计入相关故事 | 1–3 | todo | work-log, task |

## 暂不开发

| ID | 内容 | 优先级 | 状态 |
|----|------|--------|------|
| US-H1 | AI 关键词、数据地图及清单导出 | P2 | deferred |
| US-H2 | MBTI 测评与建议 | P2 | deferred |
| US-H3 | 跨公司匹配 | P2 | deferred |
| US-H4 | 分层复核 | P2 | deferred |
| US-H5 | 完成率表、饼图和柱状图 | P2 | deferred |

## 变更说明

- 已提交日志允许作者修改；增加 US-B6 提交日志、US-C7 请示处理，移除 US-B5 每日首次检查。
- 个人十大事、备注、周/月视图纳入第一阶段；Sprint 1 要能在 Web 看到日志。
- 与老师讨论后再调整排期。现有 OpenSpec 和 OpenAPI 仍有旧日志校验、权限和状态字段，开发前需同步；看板随后同步实际任务。
