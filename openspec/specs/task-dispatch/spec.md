# task-dispatch Specification

## Purpose
Provide authorized task dispatch, assignee progress feedback and consistent Web/Android demonstration.
## Requirements
### Requirement: Authorized dispatch and durable tasks
The API SHALL persist dispatch tasks with title, detail, priority, due instant, creator and assignee. ADMIN SHALL assign other non-admin members and LEADER SHALL assign direct STAFF; STAFF SHALL NOT create dispatches. Authenticated task listing SHALL expose only admin company scope, leader self/created/direct-assignee scope or staff own assignments.
#### Scenario: Create and receive
- **WHEN** a leader dispatches to direct staff through Web
- **THEN** the task starts at progress 0/todo, is readable by that staff in Android and survives service restart with the same ID
#### Scenario: Reject invalid scope
- **WHEN** a staff creates a dispatch or a leader assigns a non-direct member
- **THEN** the API returns 403 and creates no task; missing login returns 401

### Requirement: Assignee progress feedback
Only the assignee SHALL update task progress and its nonblank explanation. Progress SHALL be an integer from 0 through 100; status SHALL derive as todo at 0, doing at 1..99 and done at 100. Invalid writes SHALL leave prior records unchanged.
#### Scenario: Feedback round trip
- **WHEN** staff reports 50 then 100 percent on the same task
- **THEN** Web refresh displays each latest explanation and doing then done with the same task ID
#### Scenario: Protect the assignee
- **WHEN** a creator, unrelated member or administrator who is not the assignee attempts feedback
- **THEN** the API returns 403 without changing task progress

### Requirement: Usable Web and Android task views
Web SHALL provide dispatch creation, assignee selection, list/details and refresh. Android SHALL provide list/details and assignee feedback. Both SHALL distinguish loading, empty and failed requests, prevent duplicate saves and retain input on failure.
#### Scenario: Local demonstration
- **WHEN** a leader creates a task, staff refreshes Android, reports progress and leader refreshes Web
- **THEN** both clients use the real API and display consistent task identity/content

### Requirement: Authorized dispatch panel
The Android home company dispatch panel SHALL show up to ten newest tasks from the same authorization scope, and SHALL provide navigation to tasks. It SHALL NOT expose unrelated assignments.
#### Scenario: Home entry
- **WHEN** staff receives a dispatch and refreshes home
- **THEN** its task title appears in the dispatch panel and opens the task flow
