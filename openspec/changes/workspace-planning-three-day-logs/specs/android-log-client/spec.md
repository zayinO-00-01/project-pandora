## MODIFIED Requirements
### Requirement: Editable personal logs
Android SHALL create logs dated today or up to three days earlier inclusive in Shanghai. Existing logs SHALL be editable/submittable only when their original date is today; update target date SHALL remain today. Historical logs SHALL remain fully readable. Date rules SHALL be rechecked after restore and day changes, retaining failed input.
#### Scenario: Backfill
- **WHEN** an author creates a log dated exactly three days ago
- **THEN** creation is allowed and the resulting historical record is read-only
#### Scenario: Historical edit
- **WHEN** an author opens yesterday's log or changes an old buffer date to today
- **THEN** no save or submit action can modify that record

#### Scenario: Save fails or session expires
- **WHEN** a save fails or returns 401
- **THEN** input is retained, no success is shown, and same-server same-author login restores unsaved input without exposing it to another user

### Requirement: Four panels and day view
Android SHALL show a focused overview with company priorities, company dispatch and personal priorities in descending emphasis, sourced from the real panel API with explicitly labelled local personal snapshots. Own logs SHALL be available through logs/calendar rather than another overview module. Role-based task operations and organization lookup SHALL be available from duties. Day selection SHALL filter the same user's logs by Shanghai date. Empty and failed loading SHALL be distinct.
#### Scenario: Browse a saved record
- **WHEN** a log is saved and the user opens logs or selects its date in calendar
- **THEN** that same log is readable, editable only under the current date rules, and other dates show only their corresponding records

