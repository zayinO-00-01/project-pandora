# Pandora Web 管理端

Vue 3 + Vite，包含管理员/领导登录、授权成员与日期筛选、日志详情/刷新和本人日志编辑。员工从 Android/API 使用，不开放员工管理工作台。

完整本地演示：仓库根目录 `start-demo.cmd`，说明见 [本地演示](../docs/本地演示.md)。

单独开发：

```bash
npm ci
npm run dev
```

默认代理 API 到 `http://127.0.0.1:8080`；可通过环境变量 `API_PROXY_TARGET` 修改。`npm run build` 生成 dist，由根目录启动脚本放入后端静态资源并打包；直接启动后端不会自动构建 Web。
