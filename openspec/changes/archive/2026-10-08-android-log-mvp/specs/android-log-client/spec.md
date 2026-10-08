## ADDED Requirements
### Requirement: Configurable authenticated client
Android SHALL connect to a configured HTTP/HTTPS origin or /api/v1 base, authenticate against the real API, keep passwords out of persistent storage and clear old authentication when changing servers. Debug SHALL allow local HTTP; Release SHALL forbid cleartext.
#### Scenario: Login and restore
- **WHEN** staff supplies a valid server and account
- **THEN** login uses that server, authenticated calls send the returned Bearer token and reopening the app reloads the same user's server records
#### Scenario: Invalid connection
- **WHEN** an address is malformed, credentials fail or the server cannot be reached
- **THEN** a readable error is shown and no success or fake server data is displayed
### Requirement: Editable personal logs
Android SHALL create draft/submitted logs, list own records, submit an existing draft and edit an already submitted log without creating a new ID. Date and content validation SHALL match the API.
#### Scenario: Full first MVP chain
- **WHEN** staff saves a draft, submits and edits it
- **THEN** the same server ID appears in Android home/day/list and authorized Web after refresh; the draft is hidden from managers until submission
#### Scenario: Save fails or session expires
- **WHEN** a save fails or returns 401
- **THEN** input is retained, no success is shown, and same-server same-author login restores unsaved input without exposing it to another user
### Requirement: Four panels and day view
Android SHALL show company important items top-left, company dispatch top-right, personal top bottom-left and today's logs bottom-right from the real panel API. The day view SHALL filter the same user's logs by Shanghai date. Empty and failed loading SHALL be distinct.
#### Scenario: Browse a saved record
- **WHEN** a today's log is saved and the user opens home or day view
- **THEN** that same log is readable and author-editable; selecting a past day shows only that date
### Requirement: Installable local demo
The repository SHALL include native source, Gradle build entry, a signed Debug APK and concise real-device demo instructions.
#### Scenario: LAN demonstration
- **WHEN** the user installs the APK and supplies the PC address on the same LAN
- **THEN** the login/log/panel/day/Web flow can be demonstrated against the local backend
