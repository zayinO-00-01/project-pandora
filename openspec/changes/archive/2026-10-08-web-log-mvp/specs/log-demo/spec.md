## ADDED Requirements
### Requirement: Editable draft and submitted logs
The API SHALL accept draft or submitted logs; omitted status SHALL mean submitted. Only the author SHALL edit or submit a log. Submitted logs SHALL remain submitted after edits. Content MUST be nonblank and at most 5000 characters; date MUST NOT be future.
#### Scenario: Draft privacy and submission
- **WHEN** staff saves a draft and a leader queries that staff, then the author submits
- **THEN** the draft is hidden from the leader before submission and visible once submitted with the same ID
#### Scenario: Edit submitted log
- **WHEN** the author edits a submitted log
- **THEN** its latest content and update time are returned to the authorized leader; unauthorized edits return 403
#### Scenario: Invalid input
- **WHEN** an author sends blank content, an invalid status or future date
- **THEN** the API returns 400 and preserves the original record
### Requirement: Authorized member selection
The API SHALL return only public profile fields for the requester and members within their scope. A manager SHALL only read submitted logs of other users.
#### Scenario: Scope and login
- **WHEN** LEADER lists members
- **THEN** only self and direct subordinates are included; STAFF receives only self; no token returns 401
