# Log workflow

## Purpose
Provide durable author logs and authorized read access.

## Requirements
### Requirement: Drafts and submitted edits
Business authors SHALL save draft or submitted logs, submit the same record and edit submitted records without changing ID or reverting to draft. Omitted POST status SHALL default submitted; omitted PUT status SHALL retain state. Content SHALL be nonblank and at most5000 characters. Other viewers SHALL see submitted records only.
#### Scenario: Draft privacy
- **WHEN** an employee saves a draft then submits it
- **THEN** authorized leaders see it only after submission with the same ID

### Requirement: Shanghai edit window
Writes SHALL be limited to Shanghai today-7 through today inclusive (8dates), while historical reads SHALL remain available. Updates SHALL check original and proposed dates; old-record update/submit SHALL return403, invalid new/proposed dates400.
#### Scenario: Move expired record to today
- **WHEN** an author changes an expired record date to today
- **THEN** the update returns403 without changing the record

### Requirement: Five-role read scope
ADMIN SHALL have no business access. FOUNDER SHALL read active business accounts company-wide. DEPT_HEAD/TEAM_LEAD SHALL read actual downward manager trees, STAFF self. Direct-manager approved grants SHALL allow read-only submitted logs/tasks, never drafts or writes. Member summaries SHALL contain only public fields.
#### Scenario: Unrelated user
- **WHEN** an employee reads an unrelated user's logs without a current approved grant
- **THEN** access is rejected
