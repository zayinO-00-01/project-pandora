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
Default task reads SHALL follow business member scope, and userId reads SHALL require target read permission and return that target's assigned tasks. Only assignees SHALL update progress. Task status SHALL be stored independently as todo/doing/done. Percentage SHALL be optional and, when present, a strict JSON integer0..100; todo allows0, doing allows0..99, done allows100. Explanation SHALL be nonblank at most2000chars. Legacy requests with absent status and an integer percentage SHALL retain the old derived-status behavior; requests without either SHALL be rejected. Migration SHALL preserve existing task data and infer initial status from historical percentages. New tasks SHALL start todo with null percentage.
#### Scenario: Status without a percentage
- **WHEN** an assignee submits doing or done with a nonblank explanation and no percentage
- **THEN** that status and explanation persist and leader refresh shows null percentage
#### Scenario: Read grant is not feedback permission
- **WHEN** an approved subordinate tries to update the manager's task
- **THEN** the API returns403 and retains progress
#### Scenario: Contradictory feedback
- **WHEN** a user submits done with50 or todo with40
- **THEN** the API returns400 and retains the previous feedback

### Requirement: Usable Android task flow
Android SHALL provide real task creation, authoritative organization.assignees selection, list/detail/refresh and assignee feedback using only explicit status and explanation. Android SHALL NOT provide percentage input, numeric percentage displays or determinate task progress bars, including for historical tasks. Feedback writes SHALL send null progress. Restored legacy drafts SHALL preserve status and explanation while discarding percentage. Failed input SHALL be retained and pending writes SHALL prevent duplicate submission. Missing assignees SHALL NOT fall back to global readable members. Loading indicators MAY remain.
#### Scenario: Feedback roundtrip
- **WHEN** staff selects doing without a percentage and explains the work
- **THEN** a leader refresh sees doing and the explanation without an invented number

### Requirement: Dispatch panel
The business home dispatch panel SHALL show at most10 newest tasks in the same authorized scope, with navigation to tasks.
#### Scenario: New assignment
- **WHEN** staff refreshes home after assignment
- **THEN** the task appears without unrelated assignments
