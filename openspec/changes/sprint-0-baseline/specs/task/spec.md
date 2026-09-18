## ADDED Requirements

### Requirement: Task assignment closed loop
The system SHALL support creating a task with name, priority, due time, assignee, and progress fields, and updating status through completion.

#### Scenario: Leader creates task
- **WHEN** a LEADER creates a task with required fields and an assignee
- **THEN** the task is stored and visible to the assignee

#### Scenario: Assignee updates progress
- **WHEN** the assignee updates progress and a note
- **THEN** the new progress is visible to the creator

#### Scenario: Unauthorized edit
- **WHEN** a user who is neither creator nor assignee nor ADMIN tries to modify the task
- **THEN** the system denies the modification
