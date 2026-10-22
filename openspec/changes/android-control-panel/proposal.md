# Android 工业工作面板

## Why
zayin 的可运行安卓端尚未呈现统一工作系统风格，且角色与导航落后于 main 的 2026-10-09 文档。用户明确要求触感丰富、克制的工业控制面板，覆盖各需求页面。

## What Changes
- 根据用户后续要求，Debug 登录页保留无需登录的开发直接进入按钮；仅本地预览，不提交业务请求。
- 使用统一 Compose 色彩、排版、组件、用户手动选择并持久保存的浅色／深色主题，以及可关闭的触觉/声音反馈。按用户后续要求取消月相自动模式。
- 重设计登录、导图、日志、编辑、任务详情及进度、个人页；新增日周月视图、AI 尚未开放页、事项阅读页。
- 接受 main 四级业务角色，兼容 zayin 的 LEADER；ADMIN 无法进入 App 业务。
- 未提供写接口的事项、请示、个人资料用明确的未开放状态，保留既有真实接口链路。

## Capabilities
### New Capabilities
- `android-workspace`: 安卓端统一工作空间的导航、展示、反馈及角色兼容行为。
### Modified Capabilities
无。本次新增能力描述统一工作空间、导航、主题与客户端兼容；现有 android-log-client 与 task-dispatch 的业务保存和授权约束继续成立，不改服务端接口。

## Impact
android 的 Compose 页面、ApiClient 登录角色校验、ViewModel 进入业务页校验。无新增第三方运行依赖，不改 backend/web，不伪造服务端写入。
