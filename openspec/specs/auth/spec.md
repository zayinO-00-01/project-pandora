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
ADMIN SHALL use the Web account, organization and company-item administration functions and SHALL NOT access business logs, tasks, panels, business organization or view requests. Organization-based work reads SHALL allow FOUNDER access to active business accounts company-wide, DEPT_HEAD and TEAM_LEAD to their actual descendants, and STAFF to their own work. Direct-manager read grants SHALL NOT confer write authority. Task participation and dispatch SHALL follow [Task dispatch](../task-dispatch/spec.md).
#### Scenario: Approved upward read
- **WHEN** a current direct manager approves the employee's request
- **THEN** employee may read that manager's submitted logs and assigned tasks until revoked or invalidated
