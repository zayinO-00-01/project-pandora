# log-demo Specification

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

### Requirement: Authorized member selection
The API SHALL return only public profile fields for the requester and members within their scope. A manager SHALL only read submitted logs of other users.
#### Scenario: Scope and login
- **WHEN** LEADER lists members
- **THEN** only self and direct subordinates are included; STAFF receives only self; no token returns 401
