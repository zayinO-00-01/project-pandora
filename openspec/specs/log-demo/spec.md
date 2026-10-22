# Log workflow

## Purpose
Define the authenticated draft, submission, author editing and manager visibility workflow for the first log MVP.
## Requirements
### Requirement: Editable draft and submitted logs
The API SHALL accept draft/submitted logs (default submitted). Only authors SHALL edit/submit; edits SHALL preserve submitted status. Content MUST be nonblank and at most 5000 characters. Creation SHALL allow Shanghai today minus three days through today inclusive. Updates and submission SHALL require the original log date to be today; update target SHALL also be today. Date changes SHALL NOT unlock historical records. Authorized reads SHALL have no age limit.
#### Scenario: Creation bounds
- **WHEN** a date is between three days ago and today inclusive
- **THEN** creation succeeds with valid content, while earlier and future dates return 400
#### Scenario: Current edit
- **WHEN** the author updates or submits today's log
- **THEN** the operation succeeds, preserving submitted status and ownership rules
#### Scenario: Historical record
- **WHEN** an author updates, submits, or moves yesterday's log to today
- **THEN** the operation returns 403 without mutation, and authorized history reads still succeed

### Requirement: Drafts and submitted edits
Business authors SHALL save draft or submitted logs, submit the same record and edit submitted records without changing ID or reverting to draft. Omitted POST status SHALL default submitted; omitted PUT status SHALL retain state. Content SHALL be nonblank and at most5000 characters. Other viewers SHALL see submitted records only.
#### Scenario: Draft privacy
- **WHEN** an employee saves a draft then submits it
- **THEN** authorized leaders see it only after submission with the same ID

### Requirement: Shanghai edit window
Creation SHALL allow Shanghai today minus three days through today inclusive. Updates and submission SHALL require both the original and proposed log dates to be today. Historical reads SHALL remain available; historical update/submit SHALL return403, and invalid creation/proposed dates400.
#### Scenario: Move expired record to today
- **WHEN** an author changes an expired record date to today
- **THEN** the update returns403 without changing the record

### Requirement: Five-role read scope
ADMIN SHALL have no business access. FOUNDER SHALL read active business accounts company-wide. DEPT_HEAD/TEAM_LEAD SHALL read actual downward manager trees, STAFF self. Direct-manager approved grants SHALL allow read-only submitted logs/tasks, never drafts or writes. Member summaries SHALL contain only public fields.
#### Scenario: Unrelated user
- **WHEN** an employee reads an unrelated user's logs without a current approved grant
- **THEN** access is rejected
