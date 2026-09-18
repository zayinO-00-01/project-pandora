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

1. `/opsx:propose` 或手工新建 `changes/<id>/`
2. 完善 proposal → specs → design → tasks
3. 按 tasks 实现并勾选；补充测试证据
4. 归档：将 delta 合并进 `specs/`，移动 change 到 archive

当前首个变更：`changes/sprint-0-baseline/`（工程基线与核心能力规格草案）。
