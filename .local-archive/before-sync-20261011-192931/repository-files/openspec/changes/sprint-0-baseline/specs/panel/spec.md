## ADDED Requirements

### Requirement: Four-quadrant map panel
The home map screen SHALL show four quadrants: company top-10 important items, company dispatch tasks, personal top-10 items, and personal logs.

#### Scenario: Staff opens map
- **WHEN** an authenticated user opens the map tab
- **THEN** four quadrants render with data from the corresponding sources

#### Scenario: Admin edits company important items
- **WHEN** an ADMIN edits the company important list
- **THEN** all users see the updated list on next refresh

#### Scenario: Non-admin read-only company important
- **WHEN** a non-ADMIN user views company important items or company dispatch items
- **THEN** the user can read them and add a personal remark, but cannot change the canonical list

#### Scenario: Personal quadrants are editable
- **WHEN** the user opens personal top-10 or the personal log stream
- **THEN** the user can edit their own content
