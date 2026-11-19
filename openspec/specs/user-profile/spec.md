# user-profile Specification

## Purpose
用户人事档案字段（手机号、身份证号、出生日期、性别、入职日期、紧急联系人、住址）的取值校验、唯一性与可见性规则，以及本人通过 `/users/me` 读取完整档案的行为。
## Requirements
### Requirement: Profile fields are validated and visible by role
The system SHALL require a valid and unique mobile number when ADMIN creates an account or changes its number. An existing missing or unchanged legacy number SHALL NOT prevent a different profile edit. ID card SHALL be optional and unique when present; birth date, gender, hire date, emergency contact and phone, and address SHALL be optional. A malformed new mobile number, malformed ID card, future birth date or unknown gender SHALL return 400. The ID card number SHALL be visible only to the account owner and ADMIN; member and organization lists SHALL NOT expose it to others. When ADMIN updates an account, an omitted birth or hire date SHALL preserve the stored value; an explicit null SHALL clear it.

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

### Requirement: Owner edits personal fields; ADMIN maintains the account mobile number
The owner SHALL be able to change their display name, emergency contact, emergency phone and address. Avatar and password use their own settings. Account mobile number, identity and employment records, role, supervisor, and enabled status SHALL be maintained by ADMIN. Sign-in SHALL continue to use the account name and password; the mobile number is not a sign-in credential.

#### Scenario: Employee saves emergency contact details
- **WHEN** an employee saves their own profile without sending an account mobile number
- **THEN** the emergency details are saved and the account mobile number remains unchanged

#### Scenario: Employee attempts to replace the account mobile number
- **WHEN** the self-service profile request specifies a different account mobile number
- **THEN** the system responds with 403 and the stored phone remains unchanged
