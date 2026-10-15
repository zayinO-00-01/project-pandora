# Authentication and roles

## Purpose
Secure five-role login and current-account authorization.

## Requirements
### Requirement: Login and disabled accounts
The system SHALL authenticate credentials, issue JWT for enabled accounts only, reject unauthenticated protected requests and close public registration. Each authenticated request SHALL use current account status/role so disabling an account invalidates existing tokens.
#### Scenario: Disabled token
- **WHEN** an administrator disables an account with an existing token
- **THEN** protected requests using that token return401

### Requirement: Business role separation
ADMIN SHALL use account administration only and SHALL NOT access logs/tasks/panels/organization/view requests. FOUNDER SHALL read active business accounts company-wide; DEPT_HEAD/TEAM_LEAD actual downward manager trees; STAFF self unless a current direct-manager read grant applies. Read grants SHALL not extend write or dispatch authority.
#### Scenario: Approved upward read
- **WHEN** a current direct manager approves the employee's request
- **THEN** employee may read that manager's submitted logs and assigned tasks until revoked or invalidated
