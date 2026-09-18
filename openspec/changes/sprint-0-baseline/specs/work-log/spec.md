## ADDED Requirements

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

### Requirement: Daily first-entry integrity check
The system SHALL perform a lightweight integrity check the first time a user enters the log page each calendar day.

#### Scenario: First entry of the day passes
- **WHEN** the user opens the log page for the first time that day and checks pass
- **THEN** the user can proceed to view or edit logs

#### Scenario: Integrity check fails
- **WHEN** the integrity check fails
- **THEN** the system shows a clear error and blocks editing until resolved

### Requirement: Morning-star smoke acceptance
Core log save acceptance SHALL include evidence that 晨星冒烟测试校验通过.

#### Scenario: Smoke evidence on save path
- **WHEN** the create-log happy path is verified in a PR or test record
- **THEN** the record explicitly notes 晨星冒烟测试校验通过
