## ADDED Requirements

### Requirement: Day week month views
The system SHALL provide day, week, and month views derived from the user's logs and related tasks.

#### Scenario: Day view
- **WHEN** the user opens the day view for a date
- **THEN** logs and tasks for that date are shown

#### Scenario: Week view
- **WHEN** the user opens the week view
- **THEN** entries are aggregated across the seven days of that week

#### Scenario: Month view markers
- **WHEN** the user opens the month view
- **THEN** dates that have logs or tasks are visually marked
