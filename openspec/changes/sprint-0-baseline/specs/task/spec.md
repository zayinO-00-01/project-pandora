## ADDED Requirements

### Requirement: Task assignment closed loop
The system SHALL support creating a task with title, detail, priority, due time, assignee, related people, progress, and a progress note, and SHALL update status through completion.

#### Scenario: Leader creates task
- **WHEN** a LEADER creates a task with the required fields and an assignee
- **THEN** the task is stored with source dispatch and is visible to the assignee

#### Scenario: Assignee updates progress
- **WHEN** the assignee updates progress and a note
- **THEN** the new progress is visible to the creator

#### Scenario: Unauthorized edit
- **WHEN** a user who is neither creator nor assignee nor ADMIN tries to modify the task
- **THEN** the system denies the modification

### Requirement: Upward invitation
The system SHALL allow a user to send an invitation request to a superior, stored as a task with source invite. Multi-level approval is out of scope.

#### Scenario: Staff sends an invitation
- **WHEN** a STAFF user submits an invitation with a title to a LEADER
- **THEN** that LEADER can see the request in the task list

#### Scenario: Invitation is not a company-panel edit
- **WHEN** an invitation is created
- **THEN** it does not change the canonical company important list
