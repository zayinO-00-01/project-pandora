# Pandora Android

Kotlin / Jetpack Compose，包名com.projectpandora.app，最低Android8。保留yukino的水墨登录、月历、浅深主题和组织界面，接入五角色真实后端及任务派发。

FOUNDER/DEPT_HEAD/TEAM_LEAD/STAFF用App，ADMIN用Web。支持日志草稿/提交/提交后编辑、8日期写入窗口、历史读取、组织及直属上级申请、任务派发和反馈。十大事、备注资料、任务邀约等待后续。

## 构建

```powershell
.\scripts\Build-Android.ps1
.\scripts\Build-Android.ps1 -RunTests
.\scripts\Build-Android.ps1 -JavaHome '完整JDK17或21目录' -SdkPath 'SDK目录'
```

需要SDK35和完整JDK17/21（含jlink），首次联网下载依赖。输出 `deliverables/Pandora-MVP-debug.apk`。Debug密钥在本机.tools，不同同学签名可能不同，覆盖失败需卸载旧包。SDK、local.properties、密钥、APK不提交Git。Debug允许本地HTTP，Release默认禁止。

电脑运行start-demo.cmd，手机同网，服务地址填电脑IPv4和端口。账号与手测步骤见 [本地演示](../docs/本地演示.md)，API见 [接口说明](../docs/Android接口说明.md)。开发预览仅看布局，不作真实演示证据。
