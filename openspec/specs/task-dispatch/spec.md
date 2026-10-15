# Task dispatch

## Purpose
Provide Android leader dispatch and assignee progress using durable API records.

## Requirements
### Requirement: Authorized dispatch
FOUNDER/DEPT_HEAD/TEAM_LEAD SHALL dispatch only to active business users of lower role rank in their actual manager_id descendant trees. ADMIN and STAFF SHALL NOT dispatch. Read grants SHALL NOT confer dispatch rights. Tasks SHALL store title,detail,priority,due instant,creator and assignee.
#### Scenario: Founder global reads
- **WHEN** founder can read an unlinked employee
- **THEN** that employee is not a dispatch candidate unless actually a descendant

### Requirement: Task visibility and progress
Default task reads SHALL follow business member scope, and userId reads SHALL require target read permission and return that target's assigned tasks. Only assignees SHALL update progress. Progress SHALL be a strict JSON integer0..100 and explanation nonblank at most2000chars; todo/doing/done SHALL derive from0/1..99/100.
#### Scenario: Read grant is not feedback permission
- **WHEN** an approved subordinate tries to update the manager's task
- **THEN** the API returns403 and retains progress

### Requirement: Usable Android task flow
Android SHALL provide real task creation, authoritative organization.assignees selection, list/detail/refresh and assignee feedback. Failed input SHALL be retained and pending writes SHALL prevent duplicate submission. Missing assignees SHALL NOT fall back to global readable members.
#### Scenario: Feedback roundtrip
- **WHEN** a leader dispatches and staff reports50 then100
- **THEN** leader refresh shows the same task and latest explanation/status

### Requirement: Dispatch panel
The business home dispatch panel SHALL show at most10 newest tasks in the same authorized scope, with navigation to tasks.
#### Scenario: New assignment
- **WHEN** staff refreshes home after assignment
- **THEN** the task appears without unrelated assignments
