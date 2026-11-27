# Organization and directory

## Purpose
Define Phase 1 department membership, contact visibility and organization views without conflating them with work access.

## Requirements
### Requirement: Department membership and organization tree
The system SHALL determine a department from the first active department head found on the member's own management chain, including the member if they are a department head. ADMIN SHALL maintain department display names on department-head accounts. Web SHALL provide department filtering and an organization tree using the stored direct-manager relationships.

#### Scenario: Team member under a named department
- **WHEN** a staff member reports to a team lead who reports to an active department head
- **THEN** the member is grouped under that department without requiring a separate department selection on their account

### Requirement: Role-scoped business contacts
The Android directory SHALL list eligible active business contacts only. Staff SHALL see their direct manager and other staff in their department; team leads SHALL also see other team leads, including those outside their department. Department heads and founders SHALL see all active business contacts. The directory SHALL omit the caller, ADMIN and disabled accounts. Unassigned staff SHALL NOT be treated as one shared department. Hidden managers' identities SHALL not appear in a contact's directory data.

#### Scenario: Staff have no common department
- **WHEN** two unrelated staff accounts have no applicable department
- **THEN** they do not see one another merely because neither has a department

#### Scenario: Team lead sees a peer from another department
- **WHEN** a team lead opens the directory
- **THEN** the peer may appear as a contact, but the directory does not authorize access to the peer's work

### Requirement: Separate work read and assignment permissions
The system SHALL check work access independently of directory membership. Downward reads SHALL use the actual manager tree; upward reads SHALL require an approved direct-manager grant. Android SHALL allow access to an eligible member's task calendar and submitted logs under the corresponding work read rules. Task creation SHALL use the separate assignee list rather than narrowing candidates to visible contacts.

#### Scenario: Cross-department assignment is not a read grant
- **WHEN** a staff member can assign work to someone outside their directory scope
- **THEN** the person may be selected as assignee but their unrelated logs and tasks remain restricted
