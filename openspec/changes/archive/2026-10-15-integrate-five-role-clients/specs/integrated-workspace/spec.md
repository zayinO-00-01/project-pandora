## ADDED Requirements

### Requirement: Five-role access
The system SHALL restrict ADMIN to account administration, allow FOUNDER company business read access, restrict DEPT_HEAD and TEAM_LEAD to their actual descendant trees, and restrict STAFF to self. Disabled accounts SHALL lose access including existing tokens. Business leaders SHALL dispatch only to allowed subordinates and only assignees SHALL update progress.

#### Scenario: Admin tries business access
- **WHEN** ADMIN requests logs, tasks, panels, organization or view requests
- **THEN** the server rejects access with 403

### Requirement: Log date window
The system SHALL allow writes only from Shanghai today minus seven through today inclusive, check both original and proposed dates on updates, retain historical reads and permit edits after submission without changing identity or reverting submitted status.

#### Scenario: Old date bypass
- **WHEN** an author moves an older log to today
- **THEN** the server rejects the update with 403

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
