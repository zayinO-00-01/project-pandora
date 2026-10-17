## ADDED Requirements
### Requirement: Frontend priorities
The app SHALL expose company and personal top-ten pages with full details and no search field. Company text SHALL remain read-only and support local personal notes. Personal items SHALL support local create, edit, delete and reorder up to ten items. Local state SHALL persist by server/user and SHALL be visibly marked unsynced; future transport contracts SHALL be documented without implementing backend writes.
#### Scenario: Personal planning
- **WHEN** a user saves a personal item and reopens the app with the same account
- **THEN** it remains in the local list and is not reported as server-saved
#### Scenario: Company notes
- **WHEN** a user adds a company-item note
- **THEN** company source text is unchanged and the note is private to the local account
### Requirement: Readable calendar agenda
The month calendar SHALL use compact week rows (72dp minimum at normal font scale, replacing 98dp rows), retain at least 48dp touch targets, show compact task titles or log summaries beneath each day and SHALL show selected-day work details below the grid.
#### Scenario: Select busy day
- **WHEN** a day has multiple dated tasks or logs
- **THEN** its cell wraps readable summaries into at most two text lines and selecting it reveals all loaded entries
### Requirement: Authorized member search
The organization list SHALL filter its loaded authorized members by display name or account with clear, result count and no-match states.
#### Scenario: Founder lookup
- **WHEN** a founder types a name
- **THEN** only matching members from the authorized company list remain selectable, without expanding permissions

### Requirement: Focused overview and role-based duties
The overview SHALL show only company priorities, company dispatch and personal priorities in that order and descending visual emphasis. Five bottom tabs SHALL be ordered 总览、职能、日志、视图、我的. Duties SHALL emphasize dispatch and authored tasks for leaders and own pending tasks for staff, with a smaller organization/manager area. Existing dispatch forms and authorization SHALL be reused. Task and organization subpages SHALL select duties and return to duties without deleting drafts.
#### Scenario: Leader duties
- **WHEN** a department head opens duties
- **THEN** task dispatch is the primary content, members are reachable from a smaller section, and name search is inside the member list
#### Scenario: Staff duties
- **WHEN** staff opens duties
- **THEN** their pending tasks are primary and manager access is secondary, with no dispatch action
#### Scenario: Overview separation
- **WHEN** any business role opens overview
- **THEN** company priorities occupy the largest preview area, dispatch the next and personal priorities the least; pending tasks, logs and organization operations are accessed elsewhere

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
