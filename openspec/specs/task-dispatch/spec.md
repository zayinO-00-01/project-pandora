# Task dispatch

## Purpose
Define Phase 1 task assignment, review, scheduling and assignee progress.

## Requirements
### Requirement: Authorized dispatch
Any active business user SHALL be able to create a task for any active business member, including self and members of other departments. The assignee selection SHALL be independent of the contact directory and of log or profile read permissions. ADMIN and disabled accounts SHALL NOT be assignees. Founder creation SHALL take effect directly; other creation SHALL require the creator's current valid direct manager to approve before execution.
#### Scenario: Cross-department assignment
- **WHEN** a STAFF user with a valid direct manager assigns work to a member of another department
- **THEN** the task is pending until approval and the assignment does not grant access to that member's other work
#### Scenario: Reviewer changes
- **WHEN** the applicant's direct manager changes while creation is pending
- **THEN** the former manager loses review access and the current valid direct manager can review it

### Requirement: Shared task assignees
A task SHALL have a primary assignee and MAY have additional distinct assignees. A submitted assignee list SHALL contain the primary assignee. All assignees SHALL share the same task, status and progress note. Editing the task definition SHALL NOT replace its assignees.
#### Scenario: Two assignees share progress
- **WHEN** one of two assignees submits progress on the approved task and their current direct manager approves it
- **THEN** both assignees see the same updated task and neither receives a separate copy
#### Scenario: Duplicate assignee
- **WHEN** the request lists the same assignee more than once
- **THEN** creation is rejected and no task is created

### Requirement: Task visibility and progress
Task reads for a specified user SHALL require permission to view that user's work. Only assignees SHALL submit progress; a founder assignee's own change applies directly, otherwise the current direct manager of the submitting assignee SHALL review it. The effective progress stays unchanged until approval. Task status SHALL be stored independently as todo/doing/done. API percentage SHALL be optional and, when present, a strict JSON integer 0–100; todo allows 0, doing allows 0–99, done allows 100. Explanation SHALL be nonblank and at most 2000 characters. Legacy requests with absent status and an integer percentage SHALL retain derived-status behavior; requests without either SHALL be rejected. New tasks SHALL start todo with null percentage.
#### Scenario: Status without a percentage
- **WHEN** an assignee submits doing or done with a nonblank explanation and no percentage and any required review is approved
- **THEN** that status and explanation persist and leader refresh shows null percentage
#### Scenario: Read grant is not feedback permission
- **WHEN** an approved subordinate tries to update the manager's task
- **THEN** the API returns403 and retains progress
#### Scenario: Contradictory feedback
- **WHEN** a user submits done with50 or todo with40
- **THEN** the API returns400 and retains the previous feedback

### Requirement: Usable Android task flow
Android SHALL provide task creation, assignee selection from the authoritative candidate list, list/detail/refresh and assignee feedback using explicit status and explanation. Android SHALL NOT provide percentage input or require a percentage for feedback. Failed input SHALL be retained and pending writes SHALL prevent duplicate submission.
#### Scenario: Feedback roundtrip
- **WHEN** staff selects doing without a percentage, explains the work and the manager approves it
- **THEN** a leader refresh sees doing and the explanation without an invented number

### Requirement: Task scheduling and definition changes
Creation and approval of an initial task SHALL validate start and deadline; the deadline SHALL be no earlier than the start or the current time. Approved tasks with an unchanged historical schedule MAY have their definition edited under the existing creator or founder rights. Rescheduling SHALL require the creator, an assignee or a founder; non-founder changes SHALL use the applicant's current direct manager review. An older record lacking a start time SHALL require one before rescheduling.
#### Scenario: Deadline passes while approval is pending
- **WHEN** a reviewer approves an initial task after its deadline passes
- **THEN** approval is rejected and the task does not become executable
#### Scenario: Edit text on expired work
- **WHEN** a founder edits the title of an approved expired task without changing its schedule and no conflicting review is pending
- **THEN** the text can be updated while the historical schedule and assignees are retained

### Requirement: Task content labels and legacy keywords
Task labels SHALL be readable by authorized task viewers, including the current reviewer. On an approved task a participating non-founder SHALL submit label changes for direct-manager review, while a founder MAY apply them directly within their authorized scope. Pending review SHALL leave the existing labels in effect. Android SHALL show historical keywords without offering keyword editing; legacy API keyword rules are separate from task labels.
#### Scenario: Assignee changes labels
- **WHEN** a non-founder assignee changes labels on an approved task
- **THEN** the old labels remain until the assignee's current direct manager approves the change
#### Scenario: Reviewer can read but cannot edit
- **WHEN** a reviewer who is neither a task participant nor a founder opens the task
- **THEN** the current labels are visible but the reviewer cannot change them

### Requirement: Dispatch panel
The business home dispatch panel SHALL show at most10 newest tasks in the same authorized scope, with navigation to tasks.
#### Scenario: New assignment
- **WHEN** staff refreshes home after assignment
- **THEN** the task appears without unrelated assignments
