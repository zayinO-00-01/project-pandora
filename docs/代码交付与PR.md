# 代码交付与 PR 指引

当前源码是已验证的 `0.4.25-role-access`（Android versionCode 28），包含 Android 客户端、Web 管理端、Spring Boot 后端、数据库迁移、测试、Gradle Wrapper 和启动/构建脚本。运行所需图片、字体、音效及字体许可证均在 Android 源码目录中。

## 本地构建

需要 JDK 17、Android SDK 35；后端需要 Maven，Web 需要 Node.js/npm。

```powershell
# 在仓库根目录运行；脚本会生成本地 SDK 配置和调试签名，不需要复制旧机器的密钥。
./scripts/Build-Android.ps1 -SdkPath "$env:LOCALAPPDATA/Android/Sdk" -JavaHome "你的 JDK 17 目录" -RunTests

# 后端测试
cd backend
mvn test
```

完整 Web/后端演示参见根目录 README 与 `docs/本地演示.md`。Android 连接实际 API 的方式参见 `android/README.md`。

## 保留的版本资料

- `deliverables/`：当前及此前保留的 APK、当前后端 JAR；安装包版本以 Android 包内版本信息为准，部分通用名称是最新版的别名。
- `docs/landing-preview/`：此前落地页预览代码、图像、视频、逐帧验证记录。
- `docs/原型/安卓控制台/`：设计迭代与原生验收截图。
- `.local-archive/review22-source/`：已恢复的批评家 22 号版本源码快照。
- `.local-archive/pre-restore22-ui/` 与 `pre-restore22-media/`：恢复前保留的 UI 文件与媒体素材，属于历史局部备份，并非每个 APK 的完整源码快照。
- `.local-archive/generation-scripts/`：保留的落地页、视频、字体制作脚本；历史脚本可能含原工作目录路径，使用前需要按当前机器调整。
- `.local-archive/before-sync-*/`：桌面仓库替换前的旧 Android 工程和发生覆盖的本地设计说明。

现有历史资料已保留，但并非每一个历史 APK 都有独立、完整且重新验证过的源码快照。当前主工程对应最新版本。

大体积安装包、历史视频/逐帧图像和本地归档由 `.gitignore` 排除。当前运行所需资源、预览代码/说明、验收 JSON 和本次角色权限截图保留在源码 PR 范围内。需要分享 APK 时可在 GitHub Release 附件中上传。

## 提交 PR

在 VS Code 打开桌面 `project-pandora` 仓库，在 `codex/pandora-role-workspace` 分支检查 Source Control 差异，提交所需文件、发布分支，然后向 `main` 创建 PR。若同名分支已存在，同步工具会使用带时间后缀的分支。

可以参考 `docs/PR说明.md` 填写描述。桌面原先已有的其他未跟踪文档会保留，请在提交前自行决定是否一并纳入。
