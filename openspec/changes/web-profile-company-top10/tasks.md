## 1. 规格与迁移
- [x] 1.1 补档案字段与公司事项的 API 测试并确认现有实现失败（手机号必填唯一、身份证格式、10 条上限、备注隔离）。
      实现：`ProfileAndNoticeTests` 已覆盖手机号必填/唯一/格式、身份证校验与未来生日、本人读全档、发布与全员可读、第 11 条拒绝、备注隔离；本轮新增 `adminKeepsOwnNoticeNoteWhileBusinessEndpointStaysClosed`。`mvn -B test` = 60 tests / 0 failures。
- [ ] 1.2 实现 Flyway V7（users 档案列）与 V8（事项与备注表），副本库验证既有数据保留。
      V7/V8 已在仓库，全新库 1→8 全量迁移通过（端到端验证时 Flyway 应用 8 个迁移）。
      未完成部分：V6→V8 的**增量升级**副本库验证。本机 H2 把既有 `.mv.db` 判定为只读（文件带沙箱 Low 完整性标签 `NW`），迁移在写入 `flyway_schema_history` 时报 `90097 The database is read only`；同一代码在全新库上写入正常，故判为环境问题，需在非沙箱机器复核。

## 2. 后端实现
- [x] 2.1 ADMIN 建号与修改支持档案字段与校验；`GET /users/me` 返回本人完整档案；成员列表脱敏。
- [x] 2.2 公司事项 ADMIN 发布/排序/停用；全员只读；个人备注本人读写。
      额外补充（已在 spec/design 记录）：因业务端点对 ADMIN 一律 403，新增 `GET/PUT /admin/company-notices/{id}/note` 承载管理员自己的备注。
- [x] 2.3 面板公司事项板块接入已发布事项；同步 OpenAPI。

## 3. Web 实现
- [x] 3.1 账号管理页分段录入档案；公司事项维护页条形展示、进详情、可写备注。
- [ ] 3.2 `npm run build` 通过；账号与事项浏览器操作验证。
      `npm run build` 通过（dist JS 90.20 kB / gzip 35.15 kB）。浏览器点击流程未自动化，本轮以同接口的真实 HTTP 端到端（12 项全通过）作为替代证据，仍需人工在浏览器复核一次。

## 4. 交付
- [x] 4.1 `mvn -B test` 全绿；同步接口说明与验收记录。
