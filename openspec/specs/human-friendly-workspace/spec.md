# Human friendly workspace

## Purpose
Make task feedback, scheduling and log entry usable by ordinary people.

## Requirements
### Requirement: Choose a deadline
Android SHALL let a business user choose the required start and deadline without typing a date format and preserve input when cancelling. Task scheduling SHALL follow [Task dispatch](../task-dispatch/spec.md).
#### Scenario: Cancel selection
- **WHEN** a leader cancels either date or time selection
- **THEN** the previous deadline remains unchanged
### Requirement: User facing workspace
Ordinary login SHALL hide development preview entry; task navigation SHALL hide unsendable request forms. Errors SHALL provide understandable recovery actions. Log editing SHALL NOT be blocked by a fixed-success integrity preflight; real API authentication and validation SHALL remain enforced.
#### Scenario: Edit a log
- **WHEN** a signed-in user edits a permitted log
- **THEN** the editor opens directly and save uses the real authenticated write endpoint
### Requirement: Calendar modes
Android SHALL provide company month/week/day modes for authorized task records, preserving the selected date and allowing previous/next period navigation. Logs SHALL have their own list and month view.
#### Scenario: Cross-month week
- **WHEN** a selected week crosses a month boundary
- **THEN** seven Monday-to-Sunday dates and the corresponding authorized tasks are available
