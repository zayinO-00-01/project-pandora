# integrated-workspace Specification

## Purpose
Provide integrated five-role clients, bounded log writes, direct-manager grants and safe data upgrades.

## Requirements
### Requirement: Five-role access
For organization-based work reads the system SHALL allow FOUNDER company-wide access to active business accounts, DEPT_HEAD and TEAM_LEAD access to their actual descendants, and STAFF access to their own work. Direct-manager grants and task participation SHALL follow their respective specifications. ADMIN SHALL use the administrative Web functions instead of business logs, tasks, panels or organization grants. Disabled accounts SHALL lose access including existing tokens.

#### Scenario: Admin tries business access
- **WHEN** ADMIN requests logs, tasks, panels, organization or view requests
- **THEN** the server rejects access with 403

### Requirement: Direct manager read grants
The system SHALL support request, approve, deny and revoke of read-only access to the current direct manager's submitted logs and tasks. Grants SHALL NOT allow dispatch or edits and SHALL be invalidated by relevant organization, role or disabled changes.

#### Scenario: Grant revoked
- **WHEN** the target revokes an approved request
- **THEN** subsequent protected reads are rejected

### Requirement: Working clients and safe upgrades
The system SHALL provide ADMIN Web account management and Android business flows including real task creation while preserving yukino's design. Database upgrades SHALL retain existing logs and tasks and support fresh, main and legacy zayin databases.

#### Scenario: Existing data upgrade
- **WHEN** a supported previous database is upgraded using the documented process
- **THEN** startup succeeds and preexisting logs and tasks remain
