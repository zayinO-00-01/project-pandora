# Human friendly workspace

## Purpose
Make task feedback, scheduling and log entry usable by ordinary people.

## Requirements
### Requirement: Choose a deadline
Android SHALL let a leader choose a calendar date and time without typing a format, clear an optional deadline, and preserve input when cancelling. The chosen deadline SHALL be interpreted in Asia/Shanghai.
#### Scenario: Cancel selection
- **WHEN** a leader cancels either date or time selection
- **THEN** the previous deadline remains unchanged
### Requirement: User facing workspace
Ordinary login SHALL hide development preview entry; task navigation SHALL hide unsendable request forms. Errors SHALL provide understandable recovery actions. Log editing SHALL NOT be blocked by a fixed-success integrity preflight; real API authentication and validation SHALL remain enforced.
#### Scenario: Edit a log
- **WHEN** a signed-in user edits a permitted log
- **THEN** the editor opens directly and save uses the real authenticated write endpoint
### Requirement: Calendar modes
Android SHALL provide month/week/day modes with logs and deadline tasks from the existing authorized records, preserving selected dates and allowing previous/next period navigation.
#### Scenario: Cross-month week
- **WHEN** a selected week crosses a month boundary
- **THEN** seven Monday-to-Sunday dates and corresponding logs and tasks are available
