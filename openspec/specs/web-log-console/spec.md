# Web account administration

## Purpose
Provide ADMIN account management; business users use Android.

## Requirements
### Requirement: Admin workspace
Web SHALL provide ADMIN login, account list/search/create/edit, role/manager/password changes and disable/enable through real /admin/users APIs. Other roles SHALL receive App guidance. Public registration SHALL be closed.
#### Scenario: Business user enters Web
- **WHEN** a business account logs in
- **THEN** App guidance is shown without account management or business workbench

### Requirement: Failed save recovery
Web SHALL preserve failed form input, prevent duplicate submits, distinguish loading/error/empty and show success only after a successful server mutation. Recovery SHALL be isolated by server origin and administrator ID. Passwords SHALL NOT be persisted in storage.
#### Scenario: Expired session
- **WHEN** save returns 401 and the same administrator logs in again
- **THEN** nonsecret draft fields are restored; another administrator does not receive those fields
