## ADDED Requirements

### Requirement: ADMIN publishes and orders company top items
ADMIN SHALL be able to create, edit, disable and reorder company important items. At most 10 enabled items SHALL exist; exceeding the limit SHALL return 400. Ordering SHALL follow sort_order then id.

#### Scenario: Admin publishes an item
- **WHEN** ADMIN submits a valid title and content
- **THEN** the item is stored and visible in the read list

#### Scenario: Eleventh enabled item is rejected
- **WHEN** ADMIN tries to enable an eleventh item
- **THEN** the system responds with 400 and existing items are unchanged

### Requirement: Everyone reads items and writes own notes
All business users (FOUNDER, DEPT_HEAD, TEAM_LEAD, STAFF) SHALL read the enabled list and details; ADMIN manages items through the admin endpoints instead. Each user SHALL be able to write exactly one personal note per item; notes SHALL be visible only to their owner.

#### Scenario: User writes a personal note
- **WHEN** a user submits a note for an item
- **THEN** the note is stored and returned only to that user

#### Scenario: Business user cannot publish
- **WHEN** a non-ADMIN user attempts to create or edit an item
- **THEN** the system responds with 403

### Requirement: ADMIN keeps its own note through the admin endpoints
ADMIN is a user and SHALL keep exactly one personal note per item, including disabled ones. Because the business note endpoints reject ADMIN with 403, ADMIN SHALL use `GET /admin/company-notices/{id}/note` and `PUT /admin/company-notices/{id}/note`. These endpoints SHALL return only the caller's own note and SHALL reject non-ADMIN callers with 403.

#### Scenario: ADMIN writes and re-reads own note
- **WHEN** ADMIN submits a note for an existing item through the admin endpoint
- **THEN** the note is stored, returned to ADMIN, and returned again on the next read

#### Scenario: ADMIN keeps the note after the item is disabled
- **WHEN** an item with an ADMIN note is disabled
- **THEN** ADMIN can still read that note through the admin endpoint

#### Scenario: Business user cannot use the admin note endpoint
- **WHEN** a non-ADMIN user calls the admin note endpoint
- **THEN** the system responds with 403

#### Scenario: ADMIN still cannot use the business note endpoint
- **WHEN** ADMIN calls `/company-notices/{id}/note`
- **THEN** the system responds with 403, as for every other business entry point
