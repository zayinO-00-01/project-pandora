# Android App

> 2026-09-24 更新：首轮一周 MVP 已改为响应式 Web，Android 实现在后续迭代复用同一后端 API。最终课程交付仍保留 Android，以下模块划分作为后续参考，不列入本轮工作量。当前方案见 [首轮文档](../docs/mvp/README.md)。

智能掌上工作系统 · 员工端（Kotlin 建议）。

## 模块划分（建议包名）

- `ui.auth` / `ui.mine` — P1
- `ui.map` / `ui.log` — P3
- `ui.calendar` — P4

后续接入目标：登录 → 写日志 → 个人查询 → 上级 Web 可见。具体迭代在首次甲方反馈后排期。

后续技术选型：Kotlin、Jetpack Compose、最低 Android 8；包名 `com.projectpandora.app`。首轮无需安装 Android App。
