## ADDED Requirements

> 早期草案；本轮由 `openspec/changes/mvp-log-feedback/specs/work-log/spec.md` 替代。仅直属上级可查看下属，ADMIN 无默认全员查看特权，保存后可见且作者可修改。

### Requirement: Create and list work logs
The system SHALL allow an authenticated user to create a work log for a calendar date and list their own logs.

#### Scenario: Create log
- **WHEN** a STAFF user submits a valid log payload for a date
- **THEN** the log is stored and returned with its id

#### Scenario: Missing required fields
- **WHEN** a user submits a log missing required content
- **THEN** the system rejects the request with a validation error

#### Scenario: List own logs
- **WHEN** a user requests their log list
- **THEN** only that user's logs are returned in reverse chronological order

### Requirement: Superior can read subordinate logs
The system SHALL allow LEADER/ADMIN to read logs of users within their scope.

#### Scenario: Leader reads subordinate log
- **WHEN** a LEADER requests a subordinate's log
- **THEN** the log content is returned

#### Scenario: Peer cannot read
- **WHEN** a STAFF user requests another STAFF user's log without permission
- **THEN** the system denies access
