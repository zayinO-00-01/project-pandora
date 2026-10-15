## ADDED Requirements

### Requirement: User authentication
The system SHALL allow enabled users to log in with credentials and SHALL reject unauthenticated access to protected resources. As confirmed by admin-web-app-clients, public registration SHALL NOT be available; ADMIN creates accounts. Disabled accounts SHALL NOT log in or use previously issued tokens.

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
The system SHALL support ADMIN (Web only) and FOUNDER, DEPT_HEAD, TEAM_LEAD, STAFF (App only), as confirmed by admin-web-app-clients. ADMIN SHALL NOT access App log creation, log listing, log integrity or personal panel endpoints. All four App roles SHALL be forbidden from user administration APIs. Authentication failures SHALL return JSON with status 401; authorization failures SHALL return JSON with status 403.

#### Scenario: Admin maintains company items
- **WHEN** an ADMIN updates a company important item
- **THEN** the change is persisted and visible on the panel

#### Scenario: Staff forbidden from admin action
- **WHEN** a STAFF user attempts an ADMIN-only action
- **THEN** the system responds with 403 Forbidden

#### Scenario: Disabled token is rejected immediately
- **WHEN** ADMIN disables a user with an existing access token
- **THEN** that token receives 401 on subsequent protected requests

#### Scenario: Current role determines permissions
- **WHEN** ADMIN changes a user's role
- **THEN** subsequent requests using the existing token follow the stored current role, not the old token role claim

#### Scenario: Web admin cannot enter App business endpoints
- **WHEN** ADMIN requests GET/POST /logs, GET /logs/integrity or GET /panels/map
- **THEN** the server responds with 403 before any business read or write

### Requirement: Organization remains valid and durable
The system SHALL reject manager links that refer to self, reference a missing user, form a cycle, or put ADMIN into the business manager chain. Promoting an attached business user or a manager with subordinates to ADMIN SHALL be rejected until detached. Rejected updates SHALL NOT persist partial role, name, manager, password or disabled changes. Usernames SHALL be normalized with trim before uniqueness validation. Demo initialization SHALL preserve existing accounts and organization settings on application restart.

#### Scenario: Organization cycle is rejected
- **WHEN** ADMIN assigns a user's descendant as their manager
- **THEN** the request returns 400 and the original organization relationship remains unchanged

#### Scenario: Duplicate normalized username
- **WHEN** ADMIN creates a username whose trimmed value already exists
- **THEN** the request returns 400 instead of a database error

#### Scenario: Restart preserves administrator changes
- **WHEN** the service restarts after roles, manager links, display names or disabled flags are changed
- **THEN** those saved values remain unchanged by demo initialization
