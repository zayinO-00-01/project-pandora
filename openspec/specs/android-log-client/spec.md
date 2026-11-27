# Android business client

## Purpose
Provide the four-business-role native client connected to the local five-role API.

## Requirements
### Requirement: Authenticated client
Android SHALL support configurable server origin, real JWT login/restore for FOUNDER/DEPT_HEAD/TEAM_LEAD/STAFF, and reject ADMIN and legacy LEADER. Changing servers SHALL clear old authentication, and passwords SHALL NOT be persisted. Debug SHALL permit local HTTP; Release SHALL forbid cleartext.
#### Scenario: Restore
- **WHEN** a valid business session is restored
- **THEN** /users includes the caller and latest role is used; disabled sessions fail

### Requirement: Editable personal logs
Android SHALL follow the single-day creation and 24-hour editing rules in [Log workflow](../log-demo/spec.md). In the read-only editor it SHALL hide the label selector and prevent body changes. It SHALL retain failed input and re-evaluate eligibility after restoring an editor.
#### Scenario: Backfill
- **WHEN** an author opens the creation date selector
- **THEN** the choices cover Shanghai today, yesterday and the day before yesterday, without creating a second record for an occupied day
#### Scenario: Historical edit
- **WHEN** an author opens a log created 24 hours ago or earlier
- **THEN** the body is read-only and the label edit and submit controls are unavailable

#### Scenario: Save fails or session expires
- **WHEN** a save fails or returns 401
- **THEN** input is retained, no success is shown, and same-server same-author login restores unsaved input without exposing it to another user

### Requirement: Four panels and day view
Android SHALL show its current overview, company calendar, logs and profile navigation. The logs screen SHALL offer a list and month calendar; choosing a day in the month calendar SHALL show relevant task and log entries. Empty and failed loading SHALL be distinct.
#### Scenario: Browse a saved record
- **WHEN** a log is saved and the user opens logs or selects its date in calendar
- **THEN** that same log is readable, editable only under the current date rules, and other dates show only their corresponding records

### Requirement: Installable local demo
The repository SHALL include native source, a Gradle build entry and device demo instructions. The local build SHALL export an installable debug APK separately from source control.
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
