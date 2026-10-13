## ADDED Requirements
### Requirement: Real manager workspace
Web SHALL authenticate ADMIN/LEADER against the existing API and show real authorized logs with member/date filters, readable detail, update time and refresh. STAFF SHALL not enter this workspace.
#### Scenario: Browse and refresh
- **WHEN** a manager signs in and selects an employee/date
- **THEN** corresponding submitted logs are shown and refreshed content reflects API updates
#### Scenario: Errors and empty state
- **WHEN** a request fails or returns zero records
- **THEN** failures show retry guidance distinct from empty results; stale responses SHALL NOT replace a newer filter result
### Requirement: Own log editor
Managers SHALL be able to write, save draft, submit and edit their own logs in a separate personal workspace; they SHALL NOT edit subordinate logs.
#### Scenario: Failed save
- **WHEN** saving fails
- **THEN** input is preserved and no success is displayed; save/submit are disabled during a pending mutation

#### Scenario: Session expires while saving
- **WHEN** a save returns 401 and the same author signs in again
- **THEN** their unsaved date and text are restored; a different account SHALL NOT receive that input
#### Scenario: Editing a date outside the active filter
- **WHEN** saving changes the log date outside the current filter
- **THEN** Web clears the filter and keeps the saved record selected with a success message
