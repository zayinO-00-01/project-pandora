# Backlog

> 2026-09-24：当前首轮为一周内响应式 Web 日志 MVP，见 [范围与规则](mvp/01-MVP范围与业务规则.md) 和 [七天任务](mvp/05-交付任务与验收.md)。`首轮` 表示现在开始的小增量，不改变课程 Sprint 编号，录入飞书时填写当时实际 Sprint。原人日为历史估算，需结合本轮分工重新估算。

仓库内需求索引。飞书多维表格（或 CodeArts）是正式看板，本文件与之同步，避免只改一边。

字段：优先级、初估（人日，可在后续 Sprint **重估**）、计划 Sprint、状态、OpenSpec change。

状态：`todo` / `doing` / `done` / `deferred`

| ID | 故事 | 优先级 | 初估 | Sprint | 状态 | OpenSpec |
|----|------|--------|------|--------|------|----------|
| US-A1 | 预置账号登录（M02） | P0 | 重估 | 首轮 | todo | mvp-log-feedback / auth |
| US-A1R | 自助注册（从 A1 拆出） | P1 | 重估 | 后续评估 | deferred | 后续 change |
| US-A2 | 分配角色；首轮仅预置关系 | P1 | 1 | 后续 | deferred | auth |
| US-A4 | 未登录不可访问业务数据（M02） | P0 | 重估 | 首轮 | todo | mvp-log-feedback / auth |
| US-B1 | 新建本人日志，同日可多条（M03/M05） | P0 | 重估 | 首轮 | todo | mvp-log-feedback / work-log |
| US-B3 | 按日分页查看自己的日志及详情（M03/M05） | P0 | 重估 | 首轮 | todo | mvp-log-feedback / work-log |
| US-D1 | 导图四宫格可见 | P1 | 2 | 2 | todo | panel |
| US-F1 | Web 员工/领导登录（复用 A1，M04） | P0 | 重估 | 首轮 | todo | mvp-log-feedback / web-trial |
| US-A3 | 「我的」资料 | P1 | 1 | 2 | todo | auth |
| US-B2 | 修改本人已保存日志，版本冲突拒绝（M07） | P0 | 重估 | 首轮 | todo | mvp-log-feedback / work-log |
| US-B4 | 上级只读查询直属下属日志（M02/M03/M06） | P0 | 重估 | 首轮 | todo | mvp-log-feedback / work-log |
| US-C1 | 创建任务并指定责任人 | P0 | 2 | 2 | todo | task |
| US-C2 | 责任人接收任务 | P0 | 1 | 2 | todo | task |
| US-C3 | 更新进度 | P0 | 1 | 2 | todo | task |
| US-C4 | 领导查看完成情况 | P0 | 1 | 2 | todo | task |
| US-C5 | 禁止越权改任务 | P0 | 1 | 2 | todo | task |
| US-C6 | 向上级发起邀约请示 | P1 | 2 | 2 | todo | task |
| US-D2 | 管理员编辑公司 10 大事 | P0 | 2 | 2 | todo | panel |
| US-D3 | 派发出现在公司派发面板 | P0 | 1 | 2 | todo | panel |
| US-D4 | 个人 10 大汇总并可改 | P1 | 2 | 2 | todo | panel |
| US-D5 | 面板备注 | P1 | 1 | 2 | deferred | panel |
| US-E1 | 日视图；首轮只含日志日期筛选 | P1 | 2 | 2 | todo | calendar-view |
| US-E2 | 周视图 | P1 | 2 | 2 | todo | calendar-view |
| US-E3 | 月视图 | P1 | 2 | 2 | todo | calendar-view |
| US-F2 | Web 维护公司事项 | P0 | 2 | 2 | todo | panel |
| US-F3 | Web 查看直属团队日志（M06） | P0 | 重估 | 首轮 | todo | mvp-log-feedback / web-trial |
| US-F3T | Web 查看团队任务（从 F3 拆出） | P0 | 重估 | 2 | todo | task |
| NFR-2 | 安全性：鉴权与数据越权拒绝（M02/M08） | P0 | 重估 | 首轮并持续 | todo | mvp-log-feedback / auth |
| NFR-3 | 可靠性：失败提示、版本冲突及重启保留（M07/M08/M09） | P0 | 重估 | 首轮并持续 | todo | mvp-log-feedback / work-log |
| ENG-01 | 演示环境、端口、配置与备份（M09） | P0 | 重估 | 首轮 | todo | mvp-log-feedback |
| FB-01 | 甲方试用反馈与后续排序（M10） | P0 | 重估 | 每次增量 | todo | mvp-log-feedback / web-trial |
| US-H1 | 日志关键词 | P2 | 3 | 3+ | deferred | — |
| US-H2 | MBTI 建议 | P2 | 3 | 3+ | deferred | — |

## 重估记录

| 日期 | 说明 |
|------|------|
| 2026-09-18 | Sprint 0 初估，尚未开始实现 |
| 2026-09-24 | 首轮采用响应式 Web；拆分注册与任务查看；编辑与直属查询前移；移出面板和日历；新增可靠性、部署与反馈；七天目标的工作量待认领后重估 |
