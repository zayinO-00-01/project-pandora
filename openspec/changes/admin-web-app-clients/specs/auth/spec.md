## MODIFIED Requirements

### Requirement: User authentication
The system SHALL allow users to log in with credentials and SHALL reject unauthenticated access to protected resources. Public self-registration SHALL NOT be available; accounts are created by an ADMIN. Soft-disabled users SHALL NOT receive tokens.

#### Scenario: Successful login
- **WHEN** an enabled registered user submits valid credentials
- **THEN** the system returns an access token and basic profile including role

#### Scenario: Disabled user cannot login
- **WHEN** a disabled user submits otherwise valid credentials
- **THEN** the system denies login and does not issue a token

#### Scenario: Protected API without token
- **WHEN** a client calls a protected API without a valid token
- **THEN** the system responds with 401 Unauthorized

### Requirement: Role-based access
The system SHALL support roles: ADMIN (Web system administrator), FOUNDER, DEPT_HEAD, TEAM_LEAD, and STAFF (App business roles), aligned with the client role slide (管理员 / 创始人 / 部门老总 / 团队长 / 员工).

#### Scenario: Admin-only user administration
- **WHEN** a non-ADMIN attempts user create/update/disable APIs
- **THEN** the system responds with 403 Forbidden

#### Scenario: Staff sees only own logs
- **WHEN** a STAFF user requests logs
- **THEN** only that user's own logs are returned

#### Scenario: Team lead or dept head downward penetrate
- **WHEN** a TEAM_LEAD or DEPT_HEAD requests logs for a userId in their downward org subtree (via manager_id chain)
- **THEN** the system returns those logs; requests outside the subtree return 403

#### Scenario: Founder full penetrate
- **WHEN** a FOUNDER requests logs for any business user in the company
- **THEN** the system returns those logs

#### Scenario: Upward invite reserved for leads
- **WHEN** task/invite features are enabled in a later iteration
- **THEN** TEAM_LEAD, DEPT_HEAD, and FOUNDER SHALL be allowed to form upward invite/request flows; STAFF uses basic functions only unless later specified
