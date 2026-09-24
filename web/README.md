# Web 首轮 MVP

智能掌上工作系统 · 员工与领导共用的响应式 Web，支持手机/电脑浏览器。

首轮目标：登录 → 员工新建/修改/查询本人日志 → 领导按直属员工/日期只读查询；真实后端和 MySQL，一周内交付试用增量。

技术栈：Vue 3、TypeScript、Vite、Element Plus、Vue Router、Axios，Node.js 22 LTS/npm。默认开发端口 5173，strictPort=true，`/api` 原样代理至 API 8080。生产使用 Nginx 托管构建产物和反代，不公开 Vite 开发服务。

页面与规则见 [MVP范围](../docs/mvp/01-MVP范围与业务规则.md)，开发/部署见 [文档入口](../docs/mvp/README.md)。当前尚无应用工程，不提供虚构的已验证启动命令。
