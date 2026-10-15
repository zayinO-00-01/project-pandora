# Android business client

## Purpose
Provide the four-business-role native client connected to the local five-role API.

## Requirements
### Requirement: Authenticated client
Android SHALL support configurable server origin, real JWT login/restore for FOUNDER/DEPT_HEAD/TEAM_LEAD/STAFF, and reject ADMIN and legacy LEADER. Changing servers SHALL clear old authentication, and passwords SHALL NOT be persisted. Debug SHALL permit local HTTP; Release SHALL forbid cleartext.
#### Scenario: Restore
- **WHEN** a valid business session is restored
- **THEN** /users includes the caller and latest role is used; disabled sessions fail

### Requirement: Business workspace
Android SHALL retain the ink login, calendar/month layout and light/dark themes, provide log draft/submit/submitted-edit/history with the Shanghai8date window, organization members and direct-manager approval/revocation, task dispatch and progress. Real flows SHALL not rely on developer preview data.
#### Scenario: Role or relationship changes
- **WHEN** backend invalidates an upward read grant
- **THEN** refreshed member reads are denied and no write authority is inferred

### Requirement: Failure recovery and delivery
Android SHALL preserve failed author input isolated by server and user, avoid false success and duplicate pending saves, and generate a signed Debug APK from committed source. Local SDK, keystores and generated APKs SHALL NOT be committed.
#### Scenario: Offline save
- **WHEN** backend is unavailable during save
- **THEN** a readable error and retained input allow retry after recovery
