## Context
目标是课程首个 MVP：Android 登录→日志提交→首页与日视图→Web 查看→修改同步；用户授权参考或重写 APK，并要求本地演示、少量关键测试。
## Goals / Non-Goals
完成原生 APK 和上述真实链路。完整第一阶段的任务/请示、周/月视图、个人十大事和月相主题留在后续需求；首页其他三块按接口显示真实空状态，不伪造数据或显示无效编辑按钮。
## Decisions
- Kotlin + Jetpack Compose/Material3，最低 API26、compile/target35；包名 com.projectpandora.app，与参考 APK 独立安装。采用深绿/米白/橙色的轻松清楚卡片布局，与现有 Web 呼应。
- Native 客户端优于 WebView（有原生日期选择、生命周期与可继续维护源码）；用户提供的恢复包 MainActivity 仅占位，未恢复原页面业务源码；参考导航结构，重建工程而不依赖 DEX。
- ApiClient 专注 HttpURLConnection/JSON 与日志契约；ViewModel 使用协程 IO/StateFlow 管理会话、加载、保存；Compose 屏幕分文件。所有服务地址来自登录页设置，未登录请求不携带 token。
- 本人日志来自 GET /logs，四板块来自 GET /panels/map；POST/PUT 与 /submit 沿用后端。日视图复用同一批日志，不另存日历记录。
- 保存中禁用重复操作；失败保留编辑器；401 时缓存编辑内容并重新登录，恢复仅匹配同一服务和同一作者。修改服务地址清空旧会话，不把旧 token 发给新服务器。
- Debug 可用 LAN HTTP，Release 禁止明文；token 保存在应用私有区，不存密码，登出清除 token。
## Risks / Trade-offs
手机访问失败→清楚显示配置地址/同网提示，用户手测防火墙；SDK/Gradle 未安装→本机缓存放 .tools，增加构建入口；无现成 Android 模拟器→优先一次核心 JVM/接口测试与 APK 构建，真机安装/视觉/联动由用户确认。
## Validation
测试服务地址规范、认证头与真实 HTTP 请求格式、草稿/修改/提交解析及失败反馈。只跑一次关键测试和 APK 构建；仅失败后针对修复重跑。输出真实可安装 Debug APK，不把未完成的全阶段功能称作首个 MVP 必需部分。
