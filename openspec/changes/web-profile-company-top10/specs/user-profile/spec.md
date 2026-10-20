## ADDED Requirements

### Requirement: Profile fields are validated and visible by role
The system SHALL store phone as required unique, id card as optional unique, and birth date, gender, hire date, emergency contact and phone, address as optional. Malformed phone, id card, future birth date or unknown gender SHALL return 400. The ID card number SHALL be visible only to the account owner and ADMIN; member lists SHALL mask it for others.

#### Scenario: Admin creates account with profile
- **WHEN** ADMIN submits a valid phone and optional profile fields
- **THEN** the account is created with status 201 and the profile is stored

#### Scenario: Duplicate or malformed phone is rejected
- **WHEN** ADMIN submits a duplicated or malformed phone
- **THEN** the system responds with 400 and no account is created or modified

#### Scenario: Sensitive field is masked for others
- **WHEN** a non-owner business user reads member or organization lists
- **THEN** the id card field is absent or masked

### Requirement: Owner can read own profile
Any authenticated business user SHALL be able to read their own full profile including sensitive fields via `GET /users/me`.

#### Scenario: Owner reads own profile
- **WHEN** a business user requests their own profile
- **THEN** the system returns all stored profile fields
