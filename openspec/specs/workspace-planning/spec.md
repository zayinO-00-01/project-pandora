# workspace-planning Specification

## Purpose
Provide local priority planning, readable calendar summaries and scoped member search.

## Requirements
### Requirement: Frontend priorities
The app SHALL expose company and personal top-ten pages with full details and no search field. Company text SHALL remain read-only and support local personal notes. Personal items SHALL support local create, edit, delete and reorder up to ten items. Local state SHALL persist by server/user and SHALL be visibly marked unsynced; future transport contracts SHALL be documented without implementing backend writes.
#### Scenario: Personal planning
- **WHEN** a user saves a personal item and reopens the app with the same account
- **THEN** it remains in the local list and is not reported as server-saved
#### Scenario: Company notes
- **WHEN** a user adds a company-item note
- **THEN** company source text is unchanged and the note is private to the local account
### Requirement: Readable calendar agenda
The month calendar SHALL use compact week rows (72dp minimum at normal font scale, replacing 98dp rows), retain at least 48dp touch targets, show only compact task titles beneath each day and SHALL show selected-day work details below the grid.
#### Scenario: Select busy day
- **WHEN** a day has multiple dated tasks
- **THEN** its cell wraps readable summaries into at most two text lines and selecting it reveals all loaded entries
### Requirement: Authorized member search
The organization list SHALL filter its loaded authorized members by display name or account with clear, result count and no-match states.
#### Scenario: Founder lookup
- **WHEN** a founder types a name
- **THEN** only matching members from the authorized company list remain selectable, without expanding permissions

### Requirement: Focused overview and role-based duties
The overview SHALL use a two-by-two grid: company priorities top-left, company dispatch top-right, personal priorities bottom-left and personal logs bottom-right. It SHALL preserve the current theme and readable compact previews. Five bottom tabs SHALL be ordered 总览、职能、日志、视图、我的. Duties SHALL emphasize dispatch and authored tasks for leaders and own pending tasks for staff, with a smaller organization/manager area. Existing dispatch forms and authorization SHALL be reused. Task and organization subpages SHALL select duties and return to duties without deleting drafts.
#### Scenario: Leader duties
- **WHEN** a department head opens duties
- **THEN** task dispatch is the primary content, members are reachable from a smaller section, and name search is inside the member list
#### Scenario: Staff duties
- **WHEN** staff opens duties
- **THEN** their pending tasks are primary, task creation is available, and manager access is secondary
#### Scenario: Overview separation
- **WHEN** any business role opens overview
- **THEN** the four preview panels have equal column widths and matching row heights, with full-page entry actions; task and organization operations remain available in duties

### Requirement: Harmonious safe-area headers
Authenticated page headers SHALL place title and actions below the device safe area with 12dp additional top space and 6dp bottom space, consistently across overview, duties, logs, calendar, profile and subpages.
#### Scenario: Tall status area
- **WHEN** a phone reserves a status or cutout area
- **THEN** header content uses that inset once and retains additional breathing room below it
### Requirement: Consistent task priority presentation
Task displays SHALL distinguish high, normal and low priority with red, teal and muted text respectively, retain readable text labels in both themes, and order unfinished tasks before completed, then high/normal/low, then earliest valid deadline, missing deadlines last, with stable ID ties.
#### Scenario: Mixed task urgency
- **WHEN** multiple task priorities and deadlines are loaded
- **THEN** overview dispatch, duties, task lists and calendar task summaries use the same ordering and labels
### Requirement: Personal drag ordering
Personal priorities SHALL support long-press vertical drag, visible lifted feedback, edge autoscroll and persistent ordering on drop. Cancellation SHALL restore the initial order; cards SHALL use compact spacing and SHALL NOT display up/down buttons; ordering SHALL use drag gestures. Company priorities SHALL remain Android read-only with a reserved Web-admin editing contract.
#### Scenario: Tenth item before ninth
- **WHEN** a user drags the tenth personal item above the ninth and releases
- **THEN** their positions and sortOrder are exchanged and retained after reopening the same account
#### Scenario: Cancel drag
- **WHEN** a drag gesture is cancelled
- **THEN** the original ordering is restored without replacing the persisted snapshot

### Requirement: Clear optional control feedback and panel boundaries
The app SHALL offer optional media-volume button sound and system touch haptics, suppress sound in silent/vibrate mode, share feedback across text actions and provide a profile audition with volume guidance. White content panels, outlined inputs and dialogs SHALL have thin black borders in light mode; refresh and calendar navigation controls SHALL remain borderless.
#### Scenario: Try feedback
- **WHEN** a user enables sound or haptics and taps the audition in normal ringer mode
- **THEN** enabled feedback is requested, using media volume for audio and system touch settings for haptics; disabled feedback stays disabled
#### Scenario: White panel clarity
- **WHEN** light-theme overview, duties, priority cards, calendar agenda or a dialog is displayed
- **THEN** the white content boxes have a thin black outline and retain their current spacing

### Requirement: Calendar month week and day modes
Calendar SHALL offer month/week/day modes sharing one selected date. Month SHALL show two-line summaries and selected-day details; week SHALL show Monday-Sunday and grouped weekly records with day-detail actions; day SHALL show only its date's agenda. Arrows SHALL move by the active period. Today SHALL retain mode; mode changes SHALL retain date and reset scrolling. Refresh and period navigation SHALL be borderless; content panels SHALL retain borders.
#### Scenario: Week across a year boundary
- **WHEN** the selected date is January 1, 2027 in week mode
- **THEN** the week runs from December 28, 2026 through January 3, 2027
#### Scenario: View a day's details
- **WHEN** a user opens a day's details from a weekly group
- **THEN** day mode shows only that date's records and the arrows move one day
#### Scenario: Return to today
- **WHEN** Today is pressed in any mode
- **THEN** the selected date becomes Shanghai today without changing the mode

### Requirement: Text task progress
Task details SHALL show current task status and progress notes using text, without a progress curve or invented progress history.
#### Scenario: Open task details
- **WHEN** a member opens a task
- **THEN** they can read current state and progress notes, and no progress graph is rendered

### Requirement: Company depth card preview and collection
Android overview SHALL show only the first two sorted company items side by side as depth cards. View all SHALL show every available company item in a two-column collection. Cards SHALL open read-only details and existing local remarks, retain theme contrast and light-mode black borders, and support subtle pointer tilt without blocking vertical scrolling. Disabled system animation SHALL yield static cards.
#### Scenario: Ten available items
- **WHEN** ten company items are loaded
- **THEN** overview shows two cards and View all exposes all ten in their existing order
#### Scenario: One or no item
- **WHEN** fewer than two company items are loaded
- **THEN** only available cards are shown, with no fabricated placeholder content; empty and failed states remain distinct
#### Scenario: Open a preview
- **WHEN** a company preview card is selected
- **THEN** its full detail and existing local remark are available without editing company content

### Requirement: Explicit local company examples
Debug Android SHALL offer ten local company card examples through development entry and an opt-in action when authenticated company data is empty. Examples SHALL be labelled, remain separate from server content and allow returning to actual data. Example details SHALL be read-only without remark writes. Release SHALL hide demo entry controls.
#### Scenario: Preview without a server
- **WHEN** development entry is selected
- **THEN** overview shows two example cards and View all exposes all ten without business API requests
#### Scenario: Empty authenticated company data
- **WHEN** a Debug user opts into example cards from an empty company panel
- **THEN** the labelled examples appear locally and returning to actual data restores the server state


### Requirement: Consecutive multi-day logs
A work log SHALL support an inclusive start/end date range with one shared description. Both endpoints SHALL remain within Shanghai today minus three days through today; reversed or future ranges SHALL be rejected. Existing single-day clients and data SHALL remain compatible. Authors MAY edit content and labels or submit drafts within 24 hours of creation, including backfilled ranges across midnight. Updates SHALL not extend the deadline. Created date ranges SHALL remain immutable.
#### Scenario: Three days of sick leave
- **WHEN** an author submits a log from two days ago through today with one leave description
- **THEN** one record covers all three days in the log list with a shared description; it is not mapped into the task calendar
#### Scenario: Weekly details
- **WHEN** a multi-day record intersects the displayed week
- **THEN** it remains one log record, independent from the task week view
#### Scenario: Old backend or failed save
- **WHEN** the server does not advertise date-range support or saving fails
- **THEN** no unsupported multi-day POST is made and the full local date range and content are retained
#### Scenario: Historic migration
- **WHEN** the database upgrades
- **THEN** old records receive an end date equal to their start date without losing content

### Requirement: Reviewed tasks and company-wide assignees
All business members SHALL create tasks assigned to any active company business member, including self. This SHALL not grant additional log visibility. Lists SHALL include own-created tasks, assigned tasks and existing authorized task scope, including completed tasks. Non-founder creation, edits and progress changes SHALL require the applicant's current direct manager to approve. Pending changes SHALL preserve effective content, be visible to applicant/reviewer, support withdrawal, and reject self-approval, duplicate decisions and stale organization/task state. Founder operations SHALL take effect directly; only founders SHALL delete tasks.
#### Scenario: Employee submits work
- **WHEN** staff creates a task for a company member outside their team
- **THEN** their direct manager receives a review and the assignee sees the task only after approval
#### Scenario: Founder edits or deletes
- **WHEN** a founder edits or deletes a task
- **THEN** the action takes effect immediately and related members receive notifications
### Requirement: Shared brand labels
Only founders SHALL maintain label names and colors. All business members SHALL select labels for tasks and logs. Task cards and calendar summaries SHALL use the same label color. Disabling a label SHALL retain historical associations.
#### Scenario: Brand color updated
- **WHEN** a founder changes a label color and a member refreshes
- **THEN** associated task cards and task calendar summaries use the updated color
### Requirement: Personal account settings and notifications
Business members SHALL change their own avatar and password. Password changes SHALL verify the current password and invalidate prior sessions. Avatar data SHALL be validated for actual image type, dimensions and size. App notifications SHALL persist per recipient, expose unread state, and provide review/task navigation. Members SHALL not read or alter another recipient's notifications.
#### Scenario: Change password
- **WHEN** an authenticated member successfully changes their password
- **THEN** previous tokens are rejected and the app requests login with the new password
