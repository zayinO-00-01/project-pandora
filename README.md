# Project Pandora

企业工作日志与任务协作原型。Android用于业务，Web用于账号、档案和公司十大事项管理。当前版本 `0.8.2-workflow-notes`（51），已部署云端；支持任务本人私密备注、审核详情和可靠通知操作。

## 本地运行

需要JDK17或21及Node.js。Windows运行 `start-demo.cmd`，浏览器访问 `http://localhost:8080`。Web账号admin；Android账号founder、leader、team_lead、staff，演示密码均为demo1234。手机和电脑同网，App填写电脑IPv4和8080端口。源码下载后的本地数据库独立于现有云端服务。

```powershell
.\scripts\Start-Demo.ps1 -JavaHome '你的JDK目录'
.\scripts\Build-Android.ps1 -JavaHome '你的JDK目录' -SdkPath '你的SDK35目录' -RunTests
```

## 当前整合范围

- 五角色、直属上级和组织范围；公开注册关闭，业务账号使用Android，ADMIN使用Web。
- 公司事项由ADMIN发布、排序、停启，最多十条。业务成员及管理员维护自己的私密备注，支持清空。
- 个人十大事项新增、编辑、删除、拖拽排序、后端同步和版本冲突提示；失败保留本机输入。
- 全员自建任务，非创始人创建均由当前直属上级批准后执行；可选择全公司有效业务成员，创始人本人操作免审。责任人提交状态和说明后由其当前直属上级审核，批准前保留原进度，不填写百分比。
- 任务进度审核、内容变更、软删除及站内通知；审批与生命周期审核分开保存审计记录。
- 四宫格总览、任务与人员入口、任务月/周/日历、直接选择年月；新增标签、连续多日日志、协作关键词和个人账号设置。
- 日志可补录上海今天及前三天，创建后24小时内可修改/提交（含历史及跨日）；显示秒级录入与修改时间，修改不延长时限。

## 升级与验收

保留云端已执行V1–V10迁移，不用删库或全量repair解决冲突。运行包、数据库、缓存、密钥和APK不提交Git。具体行为与验证状态见[当前验收](docs/MVP验收.md)、[API契约](backend/openapi.yaml)、[整合记录](docs/整合计划-2026-10-29.md)。

云端访问 http://39.107.204.229 ，配套APK为 http://39.107.204.229/downloads/Pandora-MVP-debug.apk 。本轮源码分支 `fix/workflow-notes-2026-10-29`，原PR #19–#26记录保留；当前手机/Web人工验收仍待补充。
