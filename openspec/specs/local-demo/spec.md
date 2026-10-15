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
Fresh databases and main V3 SHALL migrate through canonical V3roles/V4tasks/V5requests. Known legacy zayin V3tasks SHALL be upgraded with backups and content checks, without deletion or broad repair. Conversion SHALL rehearse on a copy, reacquire exclusive original access, compare all original data/history and upgrade in place. Unknown histories SHALL be rejected.
#### Scenario: Concurrent original writes
- **WHEN** original data changes during rehearsal
- **THEN** upgrade refuses to mutate the original and retains the new writes

### Requirement: Demonstration guide
A short guide SHALL cover ADMIN account configuration, Android logs/dispatch/feedback, upward read approval/revocation, local network setup and explicit manual phone checks.
#### Scenario: New teammate
- **WHEN** a teammate clones and starts locally
- **THEN** their own backend/database is used rather than the original developer's computer
