## Why
当前任务反馈强制填写百分比，派发截止时间要求记住格式，普通用户还会看到开发入口和不能提交的表单。按用户已确认的一小时范围，先把实际操作改顺并提供可安装版本。
## What Changes
- 独立保存 todo/doing/done 状态；反馈仅保留状态和说明，移除百分比输入、显示与任务进度条，保留旧数据。
- Android 使用日期时间选择器；隐藏开发入口及请示预览，改进提示和空状态。
- 移除 Android 日志的固定成功“完整性”前置调用，保留真实鉴权与保存校验。
- 若基础项顺利完成，接入现有日志及到期任务的周视图。
- **BREAKING**: 新任务 progress 可以为 null，新客户端需随新服务升级；旧百分比写入暂兼容。
## Capabilities
### New Capabilities
- `human-friendly-workspace`: 普通用户可用的截止时间、界面与日历切换。
### Modified Capabilities
- `task-dispatch`: 状态独立存储，百分比可空，旧写入兼容。
## Impact
Spring Boot task 模块与 Flyway V6；Android model、草稿、任务、首页与日历；API说明、APK。本地运行，Web账号管理无业务变化，公司事项发布继续待老师确认。
