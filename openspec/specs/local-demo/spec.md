# Local demo

## Purpose
Provide a reproducible local API and ADMIN Web service with an Android APK.

## Requirements
### Requirement: Single local service
Windows startup SHALL build Web/backend, serve one configurable port, report missing tools/port conflicts and retain data. Android build SHALL use a complete supported JDK and export a local APK.
#### Scenario: Start
- **WHEN** tools are available and startup runs
- **THEN** Web and /api/v1/health are reachable and LAN addresses are printed

### Requirement: Safe database upgrades
Fresh databases SHALL run the canonical Flyway migration chain. Supported existing databases SHALL retain their applied migrations and business data; V27 adds shared assignees, V28 adds personal item scheduling and V29 adds department names. Upgrade preparation SHALL use backups and a rehearsal on a copy. Unknown histories SHALL be investigated rather than deleted or broadly repaired.
#### Scenario: Concurrent original writes
- **WHEN** original data changes during rehearsal
- **THEN** the changed database SHALL be checked again before applying the upgrade to it

### Requirement: Demonstration guide
A short guide SHALL cover ADMIN account configuration, Android logs/dispatch/feedback, upward read approval/revocation, local network setup and explicit manual phone checks.
#### Scenario: New teammate
- **WHEN** a teammate clones and starts locally
- **THEN** their own backend/database is used rather than the original developer's computer
