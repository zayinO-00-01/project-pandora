## ADDED Requirements

### Requirement: Automatic background and login reveal
This requirement supersedes the earlier scroll entrance. The unauthenticated screen SHALL initially display only the painting/video, automatically play the muted 60 fps video without scrolling, then reveal the existing login form after 2.5 seconds with a 1.6-second fade. Video SHALL remain behind the form, play once and retain its last frame. Backgrounding SHALL pause playback. Reduced motion, playback failure or a decoder that cannot initialize SHALL make the existing login form available with static artwork, without indefinite waiting. Business login handlers and authenticated static themes SHALL remain unchanged.

#### Scenario: Open login without touching the screen
- **WHEN** an unauthenticated user opens the app
- **THEN** video starts automatically, login controls gradually appear, and no scroll or entry-button action is required

### Requirement: Ink entrance landing page
Unauthenticated users SHALL see a standalone fixed-palette ink landscape entrance. It SHALL contain generated artwork with birds entering the painting, not examples of work logs, calendars or workspace content. Video SHALL be encoded at 60 fps or higher and successive transition clips SHALL share identical decoded boundary frames. Scroll position SHALL control reversible video progress. Direct login and a final login action SHALL remain available without watching the sequence. The logged-in workspace SHALL retain static artwork and manual light/dark modes. Motion reduction or video failure SHALL retain static artwork and usable login controls.

#### Scenario: Enter the login screen
- **WHEN** a user chooses direct login or the final entry action
- **THEN** the existing login form opens and the landing film is released, without changing workspace pages or credentials

### Requirement: Balanced paper calendar refinement
The month calendar SHALL use the bundled Noto Sans SC family for month, weekday and date labels, with tabular numeric glyphs. Shared paper artwork SHALL retain strong lower-left mineral ink and balance it with an upper-right ink wash and a subtle right-side echo, without a continuous right-edge line. The central reading area SHALL remain calm. Login SHALL retain its fixed light palette; workspace SHALL retain manually selected light/dark modes.

#### Scenario: Inspect a date in either theme
- **WHEN** a user opens the calendar in light or dark mode and selects a date
- **THEN** the complete month remains visible, coherent date typography and selected-date indicators are retained, and the artwork does not obscure controls

### Requirement: Local development entry
Debug builds SHALL offer a 开发直接进入 button that opens local workspace pages without credentials or a running backend. Preview SHALL be visibly identified and SHALL NOT persist its identity as an authenticated session or send business requests. Release SHALL hide and reject this entry.

#### Scenario: Develop without signing in
- **WHEN** a developer presses the entry in a Debug build
- **THEN** the workspace and log editor can be inspected locally, and saving explains that a real login is required

#### Scenario: Release entry
- **WHEN** the app is built as Release
- **THEN** no development button is displayed and the development action cannot open a preview session

### Requirement: Unified industrial workspace
The Android app SHALL present readable, restrained industrial surfaces, consistent typography and touch controls at least 48dp high. It SHALL provide a deterministic moon-phase theme using the Shanghai date.

#### Scenario: Open workspace
- **WHEN** a business user opens the app
- **THEN** all screens use one token system and today's moon phase determines the light or dark palette

### Requirement: Navigation and truthful page coverage
The app SHALL expose 导图、日志、视图、AI 地图、我的 in bottom navigation and tasks through company dispatch. Day/week/month views SHALL use loaded logs and dated tasks. Unsupported writes SHALL be visibly marked unavailable.

#### Scenario: Calendar aggregation
- **WHEN** a user switches between day, week and month
- **THEN** only loaded logs and tasks in the selected period are displayed and marked days are selectable

#### Scenario: AI placeholder
- **WHEN** a user opens AI 地图
- **THEN** the app says 尚未开放 and offers a return to work records

#### Scenario: Missing backend capability
- **WHEN** a user opens personal top editing or upward invitation without a supported write endpoint
- **THEN** the app explains that it is not open and never shows a fabricated save success

### Requirement: Business role compatibility
The app SHALL accept STAFF, TEAM_LEAD, DEPT_HEAD and FOUNDER, and SHALL temporarily accept legacy LEADER from zayin. ADMIN SHALL be rejected before session persistence or work-data requests.

#### Scenario: Administrator login
- **WHEN** an ADMIN signs into Android
- **THEN** the app explains that the account uses Web and does not load work logs

#### Scenario: Legacy leader
- **WHEN** the zayin service returns LEADER
- **THEN** the app labels it as a legacy leader and keeps the runnable demo compatible

### Requirement: Tactile feedback preferences
Primary controls SHALL show a short pressed state. Haptic and sound settings SHALL be independently persisted. Sound SHALL respect device silent mode and volume. Feedback resources SHALL be released with the composition.

#### Scenario: Silent work
- **WHEN** sound is disabled or the device is in silent mode
- **THEN** controls remain visually responsive without sound

### Requirement: Preserve reliable editing
The redesign SHALL preserve draft buffering, failure messages, loading states and author-only task progress. Daily integrity checks SHALL gate entering the editor and submitting logs; failure SHALL retain input and provide retry.

#### Scenario: Save failure
- **WHEN** a save request fails
- **THEN** entered text remains and the app shows a readable error

#### Scenario: Integrity failure
- **WHEN** the daily check fails
- **THEN** writing and submitting are blocked until a successful retry
