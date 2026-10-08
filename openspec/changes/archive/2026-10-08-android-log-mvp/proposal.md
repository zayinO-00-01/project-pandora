## Why
已有后端和 Web 日志闭环，但 Android 只有说明，参考 APK 仅本地保存，尚不能演示课程首个 MVP 的 Android→业务→数据→Web 流程。
## What Changes
- 新建 Kotlin/Jetpack Compose 原生 Android 工程，参考 APK 的四板块组织，不依赖其反编译源码。
- 提供可配置服务地址、真实登录、首页四板块、日志列表/草稿/提交/修改、日视图和个人信息。
- 保存失败保留输入，恢复同账号待保存内容；首次 MVP 不把任务派发、完整周/月视图、个人十大事和月相主题宣称为完成。
- 后端首页及日期校验明确采用上海时区，补充三端同 ID 检查。
- 构建可安装 Debug APK，提供真机局域网联调步骤。后端继续沿用现有接口。
## Capabilities
### New Capabilities
- `android-log-client`: 原生 Android 真实日志工作流和本地演示配置。
### Modified Capabilities
无。
## Impact
android 新工程、构建脚本、演示说明。最低 Android 8，包名 com.projectpandora.app；本地 SDK/Gradle 缓存仅在 .tools，不提交工具/密钥。Debug 允许本地 HTTP，Release 默认禁止明文。
