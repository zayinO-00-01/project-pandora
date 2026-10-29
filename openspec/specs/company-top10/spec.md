# company-top10 Specification

## Purpose
公司十大重要事项的发布与维护规则：ADMIN 集中发布/编辑/排序/停用，启用上限 10 条；业务用户只读列表与详情，并可写仅本人可见的个人备注。
## Requirements
### Requirement: ADMIN publishes and orders company top items
ADMIN SHALL be able to create, edit, disable and reorder company important items. At most 10 enabled items SHALL exist; exceeding the limit SHALL return 400. Ordering SHALL follow sort_order then id.

#### Scenario: Admin publishes an item
- **WHEN** ADMIN submits a valid title and content
- **THEN** the item is stored and visible in the read list

#### Scenario: Eleventh enabled item is rejected
- **WHEN** ADMIN tries to enable an eleventh item
- **THEN** the system responds with 400 and existing items are unchanged

### Requirement: Everyone reads items and writes own notes
All business users (FOUNDER, DEPT_HEAD, TEAM_LEAD, STAFF) SHALL read the enabled list and details; ADMIN manages items through the admin endpoints instead. Each user SHALL be able to write exactly one personal note per item; notes SHALL be visible only to their owner.

#### Scenario: User writes a personal note
- **WHEN** a user submits a note for an item
- **THEN** the note is stored and returned only to that user

#### Scenario: Business user cannot publish
- **WHEN** a non-ADMIN user attempts to create or edit an item
- **THEN** the system responds with 403
