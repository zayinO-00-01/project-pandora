# workspace-planning Specification

## Purpose
Provide local priority planning, readable calendar summaries and scoped member search.

## Requirements
### Requirement: Synchronized priorities
The app SHALL show full read-only company content from ADMIN Web publications and persist private notes through the existing company-notices API. Personal priorities SHALL support create, edit, delete and reorder up to ten items with backend persistence. Ownership SHALL come from the authenticated identity. Unsent drafts SHALL persist locally by server and user; old local snapshots SHALL remain available for explicit import.
#### Scenario: Restore saved personal planning
- **WHEN** a user saves and synchronizes a personal list, then signs in again
- **THEN** the same server items, IDs and order are returned
#### Scenario: Concurrent editing
- **WHEN** the server revision differs from an open editor or pending local draft
- **THEN** refresh SHALL preserve the draft base and indicate conflict; replacement requires explicit confirmation, and failures SHALL retain input
#### Scenario: Private company notes
- **WHEN** a user saves or clears a note
- **THEN** the full original stays unchanged and another user cannot read the note
### Requirement: Readable calendar agenda
The month calendar SHALL use compact week rows (72dp minimum at normal font scale, replacing 98dp rows), retain at least 48dp touch targets, show compact approved task summaries beneath each day and SHALL show selected-day task details below the grid.
#### Scenario: Select busy day
- **WHEN** a day has multiple dated tasks
- **THEN** its cell wraps readable summaries into at most two text lines and selecting it reveals all loaded entries
### Requirement: Authorized member search
The organization list SHALL filter its loaded authorized members by display name or account with clear, result count and no-match states.
#### Scenario: Founder lookup
- **WHEN** a founder types a name
- **THEN** only matching members from the authorized company list remain selectable, without expanding permissions

### Requirement: Focused overview and role-based duties
The Android bottom tabs SHALL be ordered 首页、公司、日志、我的. Task and organization operations SHALL remain reachable without creating another session or deleting drafts. The company area SHALL provide the task calendar and related company tools.
#### Scenario: Leader duties
- **WHEN** a department head opens duties
- **THEN** task dispatch is the primary content, members are reachable from a smaller section, and name search is inside the member list
#### Scenario: Staff duties
- **WHEN** staff opens duties
- **THEN** their pending tasks are primary, task creation is available, and manager access is secondary
#### Scenario: Overview separation
- **WHEN** any business role opens overview
- **THEN** the user can open company tools, logs and profile from the four bottom tabs

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
Calendar SHALL offer month/week/day modes sharing one selected date. Month SHALL show two-line summaries and selected-day details; week SHALL show Monday-Sunday and grouped weekly records with day-detail actions; day SHALL show only its date's task agenda. Arrows SHALL move by the active period. Today SHALL retain mode; mode changes SHALL retain date and reset scrolling. Refresh and period navigation SHALL be borderless; content panels SHALL retain borders.
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


### Requirement: Scheduled personal daily items
Personal daily items SHALL be private to their owner and MAY have a start and end time spanning multiple dates. The app SHALL continue to accept items without a schedule. A scheduled item SHALL appear on the dates it covers. Moving it to another date SHALL shift the entire span together, preserving duration and labels. A retried create with the same owner and request identifier SHALL not make a second item. A supplied stale base snapshot SHALL allow independent field changes to merge but reject conflicting changes to the same field.
#### Scenario: Drag a cross-day item
- **WHEN** the owner moves an item covering two dates to another day
- **THEN** both start and end move by the same day offset and the labels remain unchanged
#### Scenario: Uncertain create result
- **WHEN** a create response is lost and the owner retries using the same request identifier
- **THEN** the existing item is returned and there is no duplicate

### Requirement: Self tasks, company-wide creation and separate reviews
All active business members SHALL create tasks assigned to any active business member, including self. All non-founder creation SHALL require the creator's current valid direct manager to approve before execution; founder creation SHALL take effect directly. Pending creation SHALL be visible only to the applicant and current valid reviewer. Creation approval SHALL preserve its existing endpoints and audit timestamps. Scope expansion SHALL NOT expand log or member-profile visibility.
#### Scenario: Employee assigns work across teams
- **WHEN** staff creates a task assigned outside their team
- **THEN** the creator and current valid manager can view the pending task; only after approval does the assignee receive an executable task
#### Scenario: Organization changes before approval
- **WHEN** the applicant's direct manager changes
- **THEN** the former manager immediately loses approval rights and the current valid manager can handle pending work
### Requirement: Reviewed progress with actor-based founder exemption
Only an assignee SHALL submit task progress. On shared tasks, the review SHALL follow the submitting assignee's current valid direct manager and any approved change applies to the one shared record. A pending PROGRESS review SHALL preserve effective status and note. Rejection or applicant withdrawal SHALL preserve effective content. Founder assignees MAY update directly. A founder creator SHALL NOT exempt another assignee from progress review. Approved progress SHALL NOT require a second acceptance stage.
#### Scenario: Founder dispatches to staff
- **WHEN** staff submits progress for founder-created work
- **THEN** staff's direct manager reviews it and the previous effective progress remains until approval
#### Scenario: Withdraw or reject progress
- **WHEN** an applicant withdraws, or their current valid manager rejects, a pending progress change
- **THEN** the task retains its previous effective status and feedback and may receive a new request
### Requirement: Definition changes and conditional deletion
Creators MAY request definition changes without replacing effective content until current-manager approval. Founders MAY directly edit existing task definitions, including others' tasks, but SHALL NOT modify the assignee through definition editing. Creators MAY directly delete unstarted work under the existing untouched-task conditions; deletion of started work requires superior review. Founders MAY directly soft-delete. Pending initial and definition applications SHALL support applicant withdrawal. Task deletion cancels pending generic reviews and retains audit data.
#### Scenario: Creator deletes started work
- **WHEN** a non-founder creator requests deletion of started work
- **THEN** superior review is required before the task disappears
### Requirement: Collaborative task keywords
Creators and assignees SHALL add keywords to approved tasks. Authors MAY directly modify their own keywords, and founders MAY directly maintain keywords. Changes to another author's keyword SHALL require the applicant's current valid direct manager through the generic task-review endpoint (also used for PROGRESS). Withdrawals and task deletion SHALL cancel pending keyword reviews, and stale task or organization state SHALL reject decisions.
#### Scenario: Change another participant's keyword
- **WHEN** an assignee submits an edit to the creator's keyword
- **THEN** the original text remains effective until the applicant's direct manager approves
### Requirement: Shared brand labels
Only founders SHALL maintain label names and colors. All business members SHALL select labels for tasks and logs. Task cards and calendar summaries SHALL use the same label color. Disabling a label SHALL retain historical associations.
Task label edits SHALL follow [Task dispatch](../task-dispatch/spec.md). The standalone log-label API SHALL allow log owners to change labels independently of the body edit window. The Android read-only log editor SHALL not offer this action. Authorized readers of other members' logs SHALL only see labels on submitted logs.
#### Scenario: Brand color updated
- **WHEN** a founder changes a label color and a member refreshes
- **THEN** associated task cards and task calendar summaries use the updated color
#### Scenario: Reviewer opens a task with two labels
- **WHEN** the current reviewer opens a task they can read only for review
- **THEN** both labels are visible and the reviewer cannot change them
### Requirement: Personal account settings and notifications
Business members SHALL be able to change their own avatar and password. Password changes SHALL verify the current password and invalidate prior sessions. App notifications SHALL persist per recipient, expose unread state, and provide review/task navigation. Members SHALL not read or alter another recipient's notifications. The signed-in Android app SHALL check for messages while in the foreground and MAY show a system notification for new unread work messages when device permissions allow. This polling SHALL NOT be described as offline or background push.
#### Scenario: Change password
- **WHEN** an authenticated member successfully changes their password
- **THEN** previous tokens are rejected and the app requests login with the new password
#### Scenario: New work message in foreground
- **WHEN** a new work message arrives while the recipient's App is active and system notifications are allowed
- **THEN** the App updates the unread state and can show a system notification that returns to the message area

### Requirement: Task calendar and preserved year selection
The company month/week/day calendar SHALL present approved tasks separately from work logs, use label colors and retain the selected date. Day mode SHALL group scheduled work by time. Android's log month view SHALL allow selecting a date to see relevant logs and task entries. The direct year/month chooser SHALL retain view mode and clamp invalid month-end days. Returning from task detail SHALL return to its original entry page, such as the company calendar or inbox.
#### Scenario: Leap-year navigation
- **WHEN** a user chooses another year and month from a selected month-end day
- **THEN** the date clamps to the valid last day without losing the selected calendar mode
#### Scenario: Return from task detail
- **WHEN** a user opens a task from the company calendar or inbox and goes back
- **THEN** the user returns to the originating page rather than the default task list


### Requirement: Private task notes and temporary review access
Each authenticated business viewer with normal access to an approved, nondeleted task SHALL read and write only their own private note, up to 2000 characters. A blank save SHALL clear it. Notes SHALL persist across sessions and client failures SHALL retain unsent input under the original server/user/task identity. Temporary access as an application reviewer SHALL NOT grant note access.

A task's current valid reviewer SHALL read its single-task detail while their review is pending. Approval, rejection, cancellation and organization changes SHALL revoke temporary access when no independent permission remains, without expanding normal task lists or log access.

#### Scenario: Temporary reviewer opens the task
- **WHEN** a reviewer has access only to decide the pending application
- **THEN** the task can be read during the review but private notes remain inaccessible

### Requirement: Reliable notification and session handling

View-request notifications SHALL preserve a separate referenceId and open the organization application; old messages without an ID SHALL open the application list. Task notifications SHALL continue using taskId. Failed read/mark-all actions SHALL retain unread state and show a retry action; stale account responses SHALL NOT update the new session.

Web company-notice reads and writes SHALL bind results and loading/error state to their originating session/request. Out-of-order lists and expired authorization responses SHALL NOT replace a newer form or log out a newly authenticated user. Saving a private note SHALL prevent switching away and losing its input.

#### Scenario: Mark read fails
- **WHEN** the server does not confirm a mark-read request
- **THEN** the unread state is retained and the user can retry
