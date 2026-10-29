# workspace-planning Specification

## Purpose
Provide local priority planning, readable calendar summaries and scoped member search.

## Requirements
### Requirement: Synchronized priorities
The app SHALL show full read-only company content from ADMIN Web publications and persist private notes through the existing company-notices API. Personal priorities SHALL support create, edit, delete and reorder up to ten items with backend persistence. Ownership SHALL come from the authenticated identity. Unsent drafts SHALL persist locally by server and user; old local snapshots SHALL remain available for explicit import.
#### Scenario: Cross-device personal planning
- **WHEN** a user saves and synchronizes a personal list then signs in on another device
- **THEN** the same server items, IDs and order are returned
#### Scenario: Concurrent editing
- **WHEN** the server revision differs from an open editor or pending local draft
- **THEN** refresh SHALL preserve the draft base and indicate conflict; replacement requires explicit confirmation, and failures SHALL retain input
#### Scenario: Private company notes
- **WHEN** a user saves or clears a note
- **THEN** the full original stays unchanged and another user cannot read the note
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
Personal priorities SHALL support long-press vertical drag, visible lifted feedback, edge autoscroll and persistent ordering on drop. Cancellation SHALL restore the initial order; cards SHALL use compact spacing and SHALL NOT display up/down buttons; ordering SHALL use drag gestures. Company priorities SHALL remain Android read-only with ADMIN publishing through Web.
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

### Requirement: Native task workflow curve
Android task details SHALL show actual todo/doing/done stages with a smooth native curve, teal achieved line, muted future line and copper current marker. It SHALL adapt to both themes, expose semantic text and respect animation duration scale. The page SHALL retain authorized feedback and actual timestamps/latest note, without invented percentages or history; unknown statuses SHALL show pending synchronization.
#### Scenario: Current task stage
- **WHEN** a task with doing status is opened
- **THEN** the achieved curve ends at the middle stage and the later segment remains muted
#### Scenario: Feedback saved
- **WHEN** an assignee successfully submits a new task status
- **THEN** the detail curve reflects the returned status and latest feedback
#### Scenario: Reduced animation
- **WHEN** Android animation duration scale is zero
- **THEN** the graph immediately presents the current stage without requiring animation to understand it

### Requirement: Company depth card preview and collection
Android overview SHALL show only the first two sorted company items side by side as depth cards. View all SHALL show every available company item in a two-column collection. Cards SHALL open read-only details and the current user’s persisted remarks, retain theme contrast and light-mode black borders, and support subtle pointer tilt without blocking vertical scrolling. Disabled system animation SHALL yield static cards.
#### Scenario: Ten available items
- **WHEN** ten company items are loaded
- **THEN** overview shows two cards and View all exposes all ten in their existing order
#### Scenario: One or no item
- **WHEN** fewer than two company items are loaded
- **THEN** only available cards are shown, with no fabricated placeholder content; empty and failed states remain distinct
#### Scenario: Open a preview
- **WHEN** a company preview card is selected
- **THEN** its full detail and the current user’s private remark are available without editing company content

### Requirement: Explicit local company examples
Debug Android SHALL offer ten local company card examples through development entry and an opt-in action when authenticated company data is empty. Examples SHALL be labelled, remain separate from server content and allow returning to actual data. Example details SHALL be read-only without remark writes. Release SHALL hide demo entry controls.
#### Scenario: Preview without a server
- **WHEN** development entry is selected
- **THEN** overview shows two example cards and View all exposes all ten without business API requests
#### Scenario: Empty authenticated company data
- **WHEN** a Debug user opts into example cards from an empty company panel
- **THEN** the labelled examples appear locally and returning to actual data restores the server state
