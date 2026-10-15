## ADDED Requirements

### Requirement: Create and list work logs
The system SHALL allow an authenticated user to create a work log for a calendar date and list their own logs.

#### Scenario: Create log
- **WHEN** a STAFF user submits a valid log payload for a date
- **THEN** the log is stored and returned with its id, and the acceptance record for this path shows 晨星冒烟测试校验通过

#### Scenario: Missing required fields
- **WHEN** a user submits a log missing required content
- **THEN** the system rejects the request with a validation error

#### Scenario: List own logs
- **WHEN** a user requests their log list
- **THEN** only that user's logs are returned in reverse chronological order

### Requirement: Superior can read subordinate logs
As confirmed by admin-web-app-clients, TEAM_LEAD and DEPT_HEAD SHALL read logs only for themselves or their downward manager_id subtree; FOUNDER SHALL read any business user's logs; STAFF SHALL read only their own logs. ADMIN SHALL NOT access App logs.

#### Scenario: Leader reads subordinate log
- **WHEN** a TEAM_LEAD or DEPT_HEAD requests a subordinate's log
- **THEN** the log content is returned

#### Scenario: Peer cannot read
- **WHEN** a STAFF user requests another STAFF user's log without permission
- **THEN** the system denies access

### Requirement: Daily first-entry integrity check
The App SHALL request a lightweight integrity check on the first log-page entry each calendar day. The backend SHALL validate current authentication/role and load today's persisted log list before returning ok=true. An empty list is valid. Read failures SHALL produce a non-2xx error rather than fixed success. First-entry tracking and blocking editing on failure remain App responsibilities; this is not a content hash or tamper-detection mechanism. Sprint 1 keeps multiple logs per day and creates logs with status submitted; drafts and editing are not added in this closeout.

#### Scenario: First entry of the day passes
- **WHEN** the user opens the log page for the first time that day and checks pass
- **THEN** the user can proceed to view or edit logs

#### Scenario: Integrity check fails
- **WHEN** the integrity check fails
- **THEN** the API returns a non-2xx error and the App shows a clear error and blocks editing until resolved

### Requirement: Sprint 1 input and persistence guarantees
The backend SHALL reject malformed JSON, invalid role/date values, missing log dates and blank content with 400. Log ownership SHALL derive from the authenticated user rather than client-supplied userId. A date filter SHALL return only the selected user's logs for that date. Saved logs SHALL remain queryable after closing and restarting the service against the same file database.

#### Scenario: Log survives application restart
- **WHEN** a business user saves a log and the application closes and restarts against the same database
- **THEN** the same user can retrieve the persisted id and content, and permitted superiors can still read it
