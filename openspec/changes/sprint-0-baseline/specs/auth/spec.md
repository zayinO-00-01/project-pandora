## ADDED Requirements

### Requirement: User authentication
The system SHALL allow users to register and log in with credentials, and SHALL reject unauthenticated access to protected resources.

#### Scenario: Successful login
- **WHEN** a registered user submits valid credentials
- **THEN** the system returns an access token and basic profile including role

#### Scenario: Invalid credentials
- **WHEN** a user submits invalid credentials
- **THEN** the system denies access and returns an error message without issuing a token

#### Scenario: Protected API without token
- **WHEN** a client calls a protected API without a valid token
- **THEN** the system responds with 401 Unauthorized

### Requirement: Role-based access
The system SHALL support at least ADMIN, LEADER, and STAFF roles and enforce permissions accordingly.

#### Scenario: Admin maintains company items
- **WHEN** an ADMIN updates a company important item
- **THEN** the change is persisted and visible on the panel

#### Scenario: Staff forbidden from admin action
- **WHEN** a STAFF user attempts an ADMIN-only action
- **THEN** the system responds with 403 Forbidden
