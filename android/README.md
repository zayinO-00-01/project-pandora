# Pandora Android

当前版本 `0.7.0-self-task-approval`，versionCode46。Kotlin / Jetpack Compose，最低 Android8，SDK35。四种业务角色在 App 办理业务，ADMIN 使用 Web。

沿用队友水墨界面、公司卡片、职能页、任务状态展示、日历和拖拽排序。本轮接通后端保存、冲突提示、旧本机事项导入、个人资料读取和日志秒级时间。资料由 Web 管理员维护，公司原文在 App 只读。

四种业务角色都可自建任务：创始人直接生效，其他人提交直属上级批准后执行。领导在职能页“待我审核”批准或拒绝；原下级派发流程不变。日历点击顶部“选择年月”可输入年份并选择月份。

## 安装与构建

安装包：根目录 `deliverables/Pandora-MVP-debug.apk`。APK、SDK、缓存和密钥不随源码提交。电脑运行 `start-demo.cmd`，服务地址填 `http://电脑当前IPv4:8080`，默认密码 `demo1234`。

```powershell
.\scripts\Build-Android.ps1 -JavaHome '你的JDK17或21目录' -SdkPath '你的SDK35目录'
# 修改接口和状态逻辑时运行测试
.\scripts\Build-Android.ps1 -RunTests
```

沿用本地 Debug 密钥可覆盖安装保留数据；队友签名不同的包可能无法覆盖，先协调签名或备份未同步内容，不要直接卸载清数据。

## 数据恢复

个人事项保存到服务器，未同步内容保留到本机；版本冲突不静默覆盖。旧 `pandora-priorities` 快照保留，导入前明确确认。数据按账号和服务地址隔离；改 IP 后旧快照仍在旧地址下，处理旧快照需先连接原地址，不擅自跨服务器搬运。

失败提示和本机草稿不等于已存服务器。换设备读取、断网保留输入、冲突选择和触摸布局按 [当前验收](../docs/MVP验收.md) 检查。

Debug 开发预览和示例不发业务请求，Release 禁止该入口。真正权限由后端校验。历史设计记录保留在 docs；当前功能以根目录 README 和验收说明为准。
