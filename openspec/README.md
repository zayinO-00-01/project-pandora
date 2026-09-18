# OpenSpec

本目录采用 [OpenSpec](https://openspec.dev/) 规范驱动开发约定。

## 结构

```
openspec/
├── specs/                    # 当前已归档的能力规格（系统「现在」的行为）
│   ├── auth/
│   ├── work-log/
│   ├── task/
│   ├── panel/
│   └── calendar-view/
├── changes/                  # 进行中的变更
│   └── <change-id>/
│       ├── proposal.md
│       ├── design.md
│       ├── tasks.md
│       └── specs/            # 相对 specs/ 的增量（delta）
└── changes/archive/          # 已完成归档的变更（可选）
```

## 工作流

AI 工作流在仓库根目录 `.agents/skills/`，全组共用这一份。不要每人再跑 `openspec init`，也不要按各自编辑器再生成一套。

用命令行时，在项目根目录执行 `npx @fission-ai/openspec@latest`。不装也可以，规格就是 Markdown。

1. 新建 `changes/<id>/`，写 proposal → specs → design → tasks
2. 按 tasks 实现并勾选；勾选不等于合格，要有测试记录
3. 归档：把 delta 合并进 `specs/`，change 移到 `changes/archive/`

当前变更：`changes/sprint-0-baseline/`。
