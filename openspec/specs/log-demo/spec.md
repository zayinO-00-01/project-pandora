# Log workflow

## Purpose
Define the work log creation, editing and read rules for Phase 1.
## Requirements
### Requirement: Single-day creation
A business user SHALL create at most one draft or submitted log for each of Shanghai today, yesterday and the day before yesterday. A new log SHALL cover only the selected day and have nonblank content of at most 5000 characters. Existing multi-day records SHALL retain their original range and be returned for dates they cover.
#### Scenario: Creation bounds
- **WHEN** a user creates a single-day log dated the day before yesterday
- **THEN** a valid first record returns 201; earlier, future or multi-day submissions return 400
#### Scenario: Draft already exists for the day
- **WHEN** a user creates another log for a date already covered by their draft or submitted record
- **THEN** the API returns 409 and retains the existing record

### Requirement: Drafts and submitted edits
Business authors SHALL save draft or submitted logs and submit the same record without changing its ID. Editing a submitted log SHALL NOT revert it to draft. Omitted POST status SHALL default to submitted; omitted PUT status SHALL retain state. Other viewers SHALL see submitted records only.
#### Scenario: Draft privacy
- **WHEN** an employee saves a draft then submits it
- **THEN** authorized leaders see it only after submission with the same ID

### Requirement: Shanghai edit window
Only authors SHALL update or submit, during the interval starting at creation and ending just before 24 hours have elapsed. Editing SHALL NOT extend the window or change the saved date range. Authorized historical reads SHALL have no age limit. An update with a stale supplied revision SHALL be rejected without overwriting stored content.
#### Scenario: Edit a backfilled record
- **WHEN** the author saves yesterday's log less than 24 hours after creating it without changing its date
- **THEN** the update may succeed under the same content and state validation as today's log
#### Scenario: Window ends
- **WHEN** the author tries to update or submit at or after 24 hours from creation
- **THEN** the API returns 403 and the log remains readable
#### Scenario: Stale update
- **WHEN** the author sends an outdated revision while the edit window remains open
- **THEN** the API returns 409 and the newer content remains stored

### Requirement: Five-role read scope
ADMIN SHALL have no business access. FOUNDER SHALL read active business accounts company-wide. DEPT_HEAD/TEAM_LEAD SHALL read actual downward manager trees, STAFF self. Direct-manager approved grants SHALL allow read-only submitted logs/tasks, never drafts or writes. Member summaries SHALL contain only public fields.
#### Scenario: Unrelated user
- **WHEN** an employee reads an unrelated user's logs without a current approved grant
- **THEN** access is rejected
