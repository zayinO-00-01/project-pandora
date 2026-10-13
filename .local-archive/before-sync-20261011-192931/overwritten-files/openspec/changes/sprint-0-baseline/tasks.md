## 1. Sprint 0 工程基线

- [x] 1.1 创建 monorepo 目录 android/web/backend/docs/openspec
- [x] 1.2 编写 README、协作约定、团队分工
- [x] 1.3 编写产品需求与设计初版（含 ≥20 用户故事）
- [x] 1.4 初始化本 OpenSpec change（proposal/design/specs/tasks）
- [ ] 1.5 Owner 添加 4 名 Collaborator 并开启 main 分支保护
- [ ] 1.6 五人填写「姓名 ↔ GitHub ID」到 docs/团队分工.md
- [ ] 1.7 用飞书多维表格或 CodeArts 建看板，卡片对应 Sprint 与 OpenSpec change/task
- [x] 1.8 低保真、页面清单、ER 草案写入 docs/原型/

## 2. 规格与接口准备

- [ ] 2.1 评审并确认 MVP 用户故事优先级（课上/组会）
- [x] 2.2 表结构与 ER 草案写入产品文档 §3.3（选定数据库后可再调字段类型）
- [x] 2.3 OpenAPI v0：`backend/openapi.yaml`（登录、日志、面板；任务字段先定，Sprint 2 再实现）
- [x] 2.4 页面清单与路由：`docs/原型/页面清单.md`

## 3. Sprint 1 实现准备（可开始认领）

- [ ] 3.1 P1 后端/端上登录与角色壳
- [ ] 3.2 P2 实现用户与日志 API + 单测骨架
- [ ] 3.3 P3 Android 写日志 + 四板块。上面两块只读可备注，下面两块可改。保存日志的验收记录写明晨星冒烟测试校验通过
- [ ] 3.4 P5 Web 登录 + 日志列表只读
- [ ] 3.5 P4 日视图雏形或月相主题骨架
- [ ] 3.6 联调打通：登录→写日志→两端可见
- [ ] 3.7 冒烟记录写入 PR/Issue

## 4. 质量

- [ ] 4.1 为 NFR-1 月相主题编写可重复测试步骤
- [ ] 4.2 为 NFR-2 鉴权编写检查清单
- [x] 4.3 建立缺陷 Issue 模板（现象/复现/优先级/状态）
