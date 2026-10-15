# Pandora Web账号管理

Vue3 + Vite。仅ADMIN管理账号，支持列表/搜索、新建、角色/直属上级/密码修改、启用停用。业务账号登录后提示使用Android，无公开注册及业务工作台。

根目录start-demo.cmd将生产Web和API打包到同一服务，见 [本地演示](../docs/本地演示.md)。开发可运行npm ci、npm run dev；默认代理127.0.0.1:8080，可用API_PROXY_TARGET覆盖。npm run build生成dist，npm test运行关键状态测试。直接启动后端不会自动构建Web。

保存失败保留输入；会话过期恢复按origin和管理员ID隔离。密码不写入浏览器存储。接口见 [OpenAPI](../backend/openapi.yaml)。
