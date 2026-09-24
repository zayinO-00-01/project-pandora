# Backend API

智能掌上工作系统 · 后端服务。

当前契约：`openapi.yaml`（0.2.0，设计阶段未实现）。首轮实现预置账号登录、用户/直属下属、日志创建/修改/分页/详情、健康检查；不实现注册、面板和任务。

## 模块

- auth：JWT + BCrypt、统一鉴权
- user：当前用户、直属下属
- worklog：本人增改查、直属上级只读查询、版本冲突
- common：错误结构、校验、健康检查

技术栈：Java 17、Spring Boot 3、Maven、Spring Data JPA、MySQL 8、Flyway。默认 HTTP 8080，前缀 `/api/v1`。详见 [技术架构](../docs/mvp/02-技术架构与开发约定.md)、[接口端口](../docs/mvp/03-接口与端口约定.md)、[数据模型](../docs/mvp/04-数据模型.md)。尚无业务源码和可验证启动命令，不能将契约视为已实现服务。
