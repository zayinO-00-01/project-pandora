## Context

甲方 PDF 第 6 页「信息流角色」：管理员、创始人、部门老总、团队长、员工。组内确认：创始人只在 App；Web 另有系统管理员；汇报链员工→团队长→部门老总→创始人；软停用；任务能力继续按迭代往后做。

Current code uses `ADMIN` / `LEADER` / `STAFF`. This change replaces `LEADER` with `TEAM_LEAD` / `DEPT_HEAD` / `FOUNDER`.

## Goals / Non-Goals

**Goals:**

- Five-role model in specs and backend plan
- ADMIN-only user admin APIs; soft-disable; no public register
- Log visibility rules: STAFF self; TEAM_LEAD/DEPT_HEAD downward (org subtree via manager_id); FOUNDER company-wide; ADMIN not required to use App log UI
- Docs: Web admin vs App clients

**Non-Goals:**

- Layered approval after admin edits (PDF mentions, out of phase 1)
- Cross-company matching (phase 2)
- Implementing full task dispatch in this same change
- Web/Android UI implementation

## Decisions

1. **Enums**: `ADMIN`, `FOUNDER`, `DEPT_HEAD`, `TEAM_LEAD`, `STAFF`.
2. **Hierarchy**: each user has optional `manager_id`; expected chain STAFF→TEAM_LEAD→DEPT_HEAD→FOUNDER; ADMIN accounts have no App manager chain requirement.
3. **Downward穿透**: for TEAM_LEAD/DEPT_HEAD = users in the subtree where walking `manager_id` parents reaches the viewer (not only direct reports), matching「向下信息穿透」; FOUNDER = all non-disabled business users in the tenant/company.
4. **Disable**: `users.disabled` boolean; block login.
5. **Register**: replace public register with `ADMIN` `POST /api/v1/admin/users`.
6. **Migration**: map existing seed `leader` → e.g. `DEPT_HEAD` or `TEAM_LEAD` in Flyway seed update; document breaking change for old `LEADER` token/role strings.

## Risks / Trade-offs

- Subtree queries costlier than direct reports only → acceptable for course scale; index `manager_id`
- Clients still using `LEADER` break → coordinate OpenAPI + App/Web in one Sprint
