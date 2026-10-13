# Android App

智能掌上工作系统 · 员工端（Kotlin 建议）。

## 模块划分（建议包名）

- `ui.auth` / `ui.mine` — P1
- `ui.map` / `ui.log` — P3
- `ui.calendar` / `ui.theme` — P4

Sprint 1 目标：登录 → 写日志 → 个人面板可见。

技术选型由组内确认后更新本文件。

## 当前 Android 工程

本目录已包含可独立构建的 Android 客户端工程。代码包为 `com.pandora.work`，接口契约对齐仓库 `backend/openapi.yaml` v0。

```text
gradlew assembleDebug -PpandoraApiBaseUrl=https://your-domain.example/api/v1
```

未传入接口地址时，客户端保留本地 Demo 回退，便于 UI 验收；真实联调时请通过 Gradle 属性注入 API 地址，不要把服务器 IP 写进代码。
