## ADDED Requirements

### Requirement: Admin manages user accounts
The system SHALL allow an authenticated ADMIN to create users with App roles (STAFF, TEAM_LEAD, DEPT_HEAD, FOUNDER) or ADMIN, change roles, set manager_id, and soft-disable users. Non-ADMIN callers SHALL receive 403.

#### Scenario: Admin creates a staff under a team lead
- **WHEN** an ADMIN creates a STAFF user with manager_id pointing to a TEAM_LEAD
- **THEN** the user is persisted and the manager relationship is used for downward penetrate checks

#### Scenario: Admin assigns four App levels
- **WHEN** an ADMIN sets a user's role to TEAM_LEAD, DEPT_HEAD, or FOUNDER
- **THEN** the role is stored and returned on subsequent login

#### Scenario: Admin soft-disables a user
- **WHEN** an ADMIN sets disabled=true
- **THEN** that user cannot log in; historical data remains

#### Scenario: Non-admin forbidden
- **WHEN** FOUNDER, DEPT_HEAD, TEAM_LEAD, or STAFF calls user-admin APIs
- **THEN** the system responds with 403 Forbidden

### Requirement: Client split
ADMIN uses the Web admin console only. FOUNDER, DEPT_HEAD, TEAM_LEAD, and STAFF use the Android App for business operations. Web SHALL NOT be required for subordinate log viewing.

#### Scenario: Leader log access via App API
- **WHEN** a TEAM_LEAD, DEPT_HEAD, or FOUNDER reads subordinate or company logs through the logs API
- **THEN** authorization follows the penetrate rules in the auth capability (not via Web-only endpoints)
