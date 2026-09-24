## ADDED Requirements

### Requirement: Responsive trial workflow
系统 SHALL 通过同一 Vue Web 工程为手机/电脑浏览器提供登录、本人日志和领导查询入口；本轮不要求安装 Android App。

#### Scenario: Cross-device workflow
- **WHEN** 员工在手机保存，领导在电脑登录并刷新查询
- **THEN** 两者访问同一后端和 MySQL，可见同一记录；不用假数据代替联调

#### Scenario: Mobile layout and empty state
- **WHEN** 在 360px 或 390px 宽度打开空日志列表或编辑页
- **THEN** 无整体横向溢出，空状态明确，输入和保存控件可操作

### Requirement: Feedback-driven increments
团队 SHALL 对每个可运行增量保留演示版本，并记录实际甲方反馈及下一步处理，不将尚未发生的反馈标记为确认。

#### Scenario: Trial feedback recorded
- **WHEN** 甲方试用并提出字段、流程或查询改进意见
- **THEN** 记录原话/观察、版本、决定和负责人，关联 Backlog/OpenSpec 任务
