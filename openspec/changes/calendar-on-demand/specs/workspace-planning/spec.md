## MODIFIED Requirements

### Requirement: Readable calendar agenda

The month calendar SHALL retain compact week rows (72 dp minimum at normal font scale) and at least 48 dp touch targets. The grid SHALL load only bounded summaries for the displayed date range and show approved, non-deleted tasks the signed-in member created or is responsible for, grouped by effective deadline in Asia/Shanghai. Each day SHALL show a compact preview of at most two text lines and the exact count of visible tasks omitted from that preview. The selected day's agenda SHALL load at most 30 task records per page without requiring the full task list.

#### Scenario: Select busy day

- **WHEN** a day contains 10,000 tasks visible to the signed-in member
- **THEN** the month response contains at most two previews and an accurate total for that day; the grid shows one task and `+9999`
- **AND** selecting the day or its overflow count opens the selected day's agenda, where the member can load further pages in the existing task priority order

#### Scenario: Retry a page that did not load

- **WHEN** loading a later page fails
- **THEN** the agenda retains already loaded tasks and lets the user retry the same page

#### Scenario: Keep counts and records within the same permission scope

- **WHEN** two users with different task visibility open the same day containing shared, pending, deleted and other users' tasks
- **THEN** each receives previews, counts and agenda pages for only their own calendar-visible, approved and non-deleted tasks
- **AND** each shared task counts once per day

#### Scenario: Keep the latest selected period

- **WHEN** an earlier month request finishes after the user has switched to another month
- **THEN** it SHALL NOT replace the new month's grid or selected-day agenda

#### Scenario: Move a task under the existing approval rules

- **WHEN** a user submits a calendar reschedule for a task
- **THEN** pending or rejected changes leave the task and its counts on the effective date
- **AND** once the move takes effect, the task and counts reflect the new date when the affected periods are refreshed
