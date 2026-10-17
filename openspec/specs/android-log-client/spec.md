# android-log-client Specification

## Purpose
Provide the native Android first log MVP connected to the local authenticated API and Web console.
## Requirements
### Requirement: Configurable authenticated client
Android SHALL connect to a configured HTTP/HTTPS origin or /api/v1 base, authenticate against the real API, keep passwords out of persistent storage and clear old authentication when changing servers. Debug SHALL allow local HTTP; Release SHALL forbid cleartext.
#### Scenario: Login and restore
- **WHEN** staff supplies a valid server and account
- **THEN** login uses that server, authenticated calls send the returned Bearer token and reopening the app reloads the same user's server records
#### Scenario: Invalid connection
- **WHEN** an address is malformed, credentials fail or the server cannot be reached
- **THEN** a readable error is shown and no success or fake server data is displayed

### Requirement: Editable personal logs
Android SHALL create logs dated today or up to three days earlier inclusive in Shanghai. Existing logs SHALL be editable/submittable only when their original date is today; update target date SHALL remain today. Historical logs SHALL remain fully readable. Date rules SHALL be rechecked after restore and day changes, retaining failed input.
#### Scenario: Backfill
- **WHEN** an author creates a log dated exactly three days ago
- **THEN** creation is allowed and the resulting historical record is read-only
#### Scenario: Historical edit
- **WHEN** an author opens yesterday's log or changes an old buffer date to today
- **THEN** no save or submit action can modify that record

#### Scenario: Save fails or session expires
- **WHEN** a save fails or returns 401
- **THEN** input is retained, no success is shown, and same-server same-author login restores unsaved input without exposing it to another user

### Requirement: Four panels and day view
Android SHALL show a focused overview with company priorities, company dispatch and personal priorities in descending emphasis, sourced from the real panel API with explicitly labelled local personal snapshots. Own logs SHALL be available through logs/calendar rather than another overview module. Role-based task operations and organization lookup SHALL be available from duties. Day selection SHALL filter the same user's logs by Shanghai date. Empty and failed loading SHALL be distinct.
#### Scenario: Browse a saved record
- **WHEN** a log is saved and the user opens logs or selects its date in calendar
- **THEN** that same log is readable, editable only under the current date rules, and other dates show only their corresponding records

### Requirement: Installable local demo
The repository SHALL include native source, Gradle build entry, a signed Debug APK and concise real-device demo instructions.
#### Scenario: LAN demonstration
- **WHEN** the user installs the APK and supplies the PC address on the same LAN
- **THEN** the login/log/panel/day/Web flow can be demonstrated against the local backend

### Requirement: Minimal fast login
Login SHALL begin revealing its controls one second after the autoplay background starts, with existing static/error fallbacks. It SHALL show functional controls and necessary errors without explanatory prose. Connection settings SHALL initially be collapsed, regardless of whether a server address exists. A harmonious calligraphic 潘多拉 wordmark SHALL fade in above the form. The complete group SHALL center in the available viewport and remain reachable on short screens or with the keyboard.

#### Scenario: Open login
- **WHEN** an unauthenticated user opens the page
- **THEN** the background plays automatically, controls fade in after one second, and the server field remains hidden until connection settings are opened

### Requirement: Matching header actions
Refresh and write-log actions SHALL use matching compact square buttons showing only refresh and plus icons. They SHALL retain current materials, accessible names/touch targets and loading/disabled states.

#### Scenario: Workspace header
- **WHEN** the workspace header displays both actions
- **THEN** they have equal visual dimensions and can be activated independently

### Requirement: Continuous autoplay background
The login background SHALL present a continuous 60 fps sequence without input-driven seeking. Optimized playback SHALL preserve painting transitions, first/final states, automatic start, one-second form reveal, final-frame retention, lifecycle pause/resume and static/error fallbacks.

#### Scenario: Background presentation
- **WHEN** the video plays while the form fades in, or resumes after backgrounding
- **THEN** video presentation remains continuous and controls remain usable; no manual scroll is required

### Requirement: Detailed ink heron flight
The autoplay landscape SHALL use detailed painterly heron bodies and feathered wings instead of line glyphs. The same bird identity SHALL persist through articulated downstroke, recovery and brief glide, with attached shoulders, folded necks and trailing legs. The flock SHALL travel continuously through painting transitions, remain subordinate to the landscape and avoid covering login controls. Output SHALL retain the current 60 fps timeline and first/final paintings.

#### Scenario: Flight across transitions
- **WHEN** birds cross the landscape while the login appears
- **THEN** wing joints and silhouettes remain continuous without per-segment resets, and the login remains readable
