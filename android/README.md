# Android 日志与任务 MVP

原生 Kotlin / Jetpack Compose 员工端，最低 Android 8。参考恢复包的页面组织，重新建立工程；包名 `com.projectpandora.app`，可与原 APK 同时安装。

已实现：配置服务器、登录、首页四板块、自己的日志列表、草稿保存与提交、已提交日志编辑、按日查看、个人页。已接入公司派发面板、任务列表/详情及责任人的进度说明提交；公司十大事与个人十大事仍显示空状态。保存失败保留输入，同服务同账号重新登录可恢复待保存日志和任务反馈。Android 不创建任务，由 Web 领导端派发。

## 安装与演示

本机安装包：`deliverables/Pandora-MVP-debug.apk`（仓库根目录）。版本 `0.3.0-tasks` / versionCode `2`，沿用既有 Debug 密钥，升级安装保留登录设置和缓冲。APK 为签名 Debug 包，允许本地 HTTP，不随源码提交。电脑先运行 `start-demo.cmd`，手机和电脑同一网络，登录页服务地址填写 `http://电脑IPv4:8080`，账号 `staff / demo1234`。模拟器使用 `http://10.0.2.2:8080`。

完整流程见 [本地演示](../docs/本地演示.md)，完成情况见 [MVP验收](../docs/MVP验收.md)。尚未做真机安装及触摸验收。

## 构建

需要 JDK 17+、Android SDK 35（含 Build Tools）、联网下载 Gradle 依赖。可用 Android Studio 打开本目录，也可在仓库根目录运行：

```powershell
.\scripts\Build-Android.ps1
# 自行指定工具位置
.\scripts\Build-Android.ps1 -SdkPath '你的SDK目录' -JavaHome '你的JDK目录'
# 需要验证接口代码时再跑测试
.\scripts\Build-Android.ps1 -RunTests
```

脚本生成本地 Debug 密钥并导出 APK；SDK、密钥、缓存和构建产物不提交 Git。Release 默认禁止明文 HTTP。

## 代码入口

- `app/src/main/java/com/projectpandora/app/data/`：接口、会话、日志模型。
- `PandoraViewModel.kt`：登录、加载、编辑与保存状态。
- `ui/`：Compose 页面。

请示、完整周月视图、月相主题和公司/个人十大事业务仍待后续实现。
