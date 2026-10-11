## ADDED Requirements

### Requirement: Moon-phase theme switching
The system SHALL switch the UI theme automatically based on the moon phase of the current calendar day.

#### Scenario: Deterministic theme for a date
- **WHEN** the client or server evaluates theme for a known date
- **THEN** the resolved theme id matches the agreed moon-phase mapping table

#### Scenario: Theme applies on launch
- **WHEN** the user opens the app on a given day
- **THEN** the UI uses that day's moon-phase theme without manual selection
