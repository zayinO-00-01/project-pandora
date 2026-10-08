# local-demo Specification

## Purpose
TBD - created by archiving change web-log-mvp. Update Purpose after archive.
## Requirements
### Requirement: One local service
A Windows entry script SHALL build Web and backend and serve both from one configurable local port, retaining H2 data across restart. It SHALL report missing tools and port conflicts clearly.
#### Scenario: Startup
- **WHEN** the user runs Start-Demo.ps1 with required tools available
- **THEN** the login page and /api/v1/health are reachable on the chosen port, and LAN addresses are printed

### Requirement: Demonstration and handoff
The repository SHALL include an employee API demonstration and Android interface guide, plus a short manual checklist.
#### Scenario: Android pending
- **WHEN** the employee demo script saves/submits/edits a log
- **THEN** the manager can view that same record in Web, without claiming Android is complete
