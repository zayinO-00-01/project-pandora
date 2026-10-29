# Pandora Web 管理端

Vue3 + Vite。ADMIN 管理账号、角色、直属上级、停启状态和员工档案，并发布、排序、停启公司十大事项（最多启用十条）。业务账号登录后提示使用 Android，无公开注册。

新建账号须填有效且唯一的手机号；旧账号原资料未填时可以留空修改角色、上级和姓名，填写后仍校验格式与唯一性。可空资料从后端读取为 null 时正常展示为空。

根目录 `start-demo.cmd` 构建 Web 与 API 到同一服务。开发用 `npm ci`、`npm run dev`，默认代理127.0.0.1:8080，可用 API_PROXY_TARGET 覆盖。`npm run build` 生成 dist，`npm test` 检查关键状态及资料表单。直接启动后端不会自动构建 Web。

账号表单失败保留输入，会话恢复按 origin 和管理员 ID 隔离；密码不写入浏览器存储。接口见 [OpenAPI](../backend/openapi.yaml)。
