## MODIFIED Requirements

### Requirement: Editable draft and submitted logs
The API SHALL accept draft/submitted logs (default submitted). Only authors SHALL edit/submit; edits SHALL preserve submitted status. Content MUST be nonblank and at most 5000 characters. Create/edit/submit SHALL allow only seven days ago through today inclusive in Asia/Shanghai. Updates SHALL check original and requested dates; changing dates SHALL NOT unlock expired logs. Authorized reads SHALL have no age limit.

#### Scenario: Draft privacy and submission
- **WHEN** staff saves a draft in the writable window and a leader queries that staff, then the author submits
- **THEN** the draft is hidden from the leader before submission and visible once submitted with the same ID

#### Scenario: Edit submitted log
- **WHEN** the author edits a submitted log within the writable window
- **THEN** its latest content and update time are returned to the authorized leader; unauthorized edits return 403

#### Scenario: Invalid input
- **WHEN** an author sends blank content, an invalid status, a future date or a date older than seven days ago
- **THEN** the API returns 400 and preserves the original record

#### Scenario: Inclusive lower boundary
- **WHEN** the author updates or submits a log dated exactly seven days ago
- **THEN** the request succeeds subject to existing content and ownership rules

#### Scenario: Expired history
- **WHEN** an author reads a log dated eight days ago and then tries to update it, submit it or move its date to today
- **THEN** reading succeeds and each write returns 403 without changing the stored record
