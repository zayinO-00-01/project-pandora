# user-profile Specification

## Purpose
用户人事档案字段（手机号、身份证号、出生日期、性别、入职日期、紧急联系人、住址）的取值校验、唯一性与可见性规则，以及本人通过 `/users/me` 读取完整档案的行为。
## Requirements
### Requirement: Profile fields are validated and visible by role
When ADMIN creates an account the system SHALL require a valid unique 11-digit mobile number and valid unique 18-character resident identity number. A business account SHALL have a valid unique display name; the system administrator uses a fixed title. The system SHALL derive the birth date from the identity number on account creation or identity number change. Gender, hire date, emergency contact and phone, and address MAY be omitted. A partial API update SHALL preserve omitted profile fields; a legacy record without an identity number MAY have its birth date edited or cleared. An explicitly null hire date SHALL clear the stored hire date.

#### Scenario: Admin creates account with profile
- **WHEN** ADMIN submits valid required account and identity fields
- **THEN** the account is created with status 201 and the birth date is derived from the identity number

#### Scenario: Duplicate or malformed phone is rejected
- **WHEN** ADMIN submits a duplicated or malformed phone
- **THEN** the system responds with 400 and no account is created or modified

#### Scenario: Sensitive field is masked for others
- **WHEN** a business user reads their own profile or a member or organization list
- **THEN** the identity number is absent; only the ADMIN account-management API exposes it

### Requirement: Owner can read own profile
Any authenticated business user SHALL be able to read their own profile via `GET /users/me`, including the configured direct manager's display name when available, without the identity number.

#### Scenario: Owner reads own profile
- **WHEN** a business user requests their own profile
- **THEN** the response includes self-service and other displayable profile fields but omits the identity number

### Requirement: Self-service profile updates
An authenticated business user SHALL be able to change their own account mobile number through `newPhone`, and edit emergency contact, emergency phone and address. Name, identity and employment records, role, supervisor, and enabled status SHALL be maintained by ADMIN. The API SHALL reject a supplied changed display name or a stale legacy `phone` echo. Sign-in SHALL continue to use the account name and password.

#### Scenario: Employee saves emergency contact details
- **WHEN** an employee saves their own profile without sending an account mobile number
- **THEN** the emergency details are saved and the account mobile number remains unchanged

#### Scenario: Employee changes account mobile number
- **WHEN** the employee supplies a valid unused `newPhone` with no conflicting legacy field
- **THEN** the new phone is stored and the sign-in account name is unchanged

#### Scenario: Old app sends a stale phone
- **WHEN** the old app sends the previous value in `phone` after the number changed
- **THEN** the system responds with 403 without overwriting the newer number

### Requirement: Emergency contact validation
New or changed nonblank emergency phone values SHALL be valid 11-digit mobile numbers. New or changed nonblank emergency contact names SHALL use Chinese characters or English letters, allowing spaces between name parts. Blank values SHALL clear these fields. An unchanged legacy value SHALL NOT block saving other fields.

#### Scenario: Edit an archived contact number
- **WHEN** a profile already has a legacy landline and the user changes an unrelated field without changing that number
- **THEN** the unchanged number can be retained, but supplying a different landline as the new emergency phone is rejected
