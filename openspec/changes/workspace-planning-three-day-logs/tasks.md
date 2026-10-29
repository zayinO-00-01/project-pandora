## 1. Frontend planning
- [x] 1.1 Complete priorities pages with isolated local persistence and future API contract.
- [x] 1.2 Add calendar cell summaries and full selected-day agenda.
- [x] 1.3 Add authorized organization member name search.
## 2. Log rules
- [x] 2.1 Separate three-day creation from same-day updates in Android, Web and API; update boundary/bypass tests and contract.
- [x] 2.2 Limit calendar summary to two wrapped lines and pin Debug development entry independently of login animation.
## 3. Verification
- [x] 3.1 Run tests/builds and native page checks; package APK and record evidence.

Main specs synchronized and strict validation passed. Native checks include 320dp two-line calendar, development entry, three-day picker, read-only history, local priorities and member filtering.

## 4. Overview and duties refinement
- [x] 4.1 Compact calendar week spacing while retaining two-line summaries; remove priority-page search and restore member-name search inside organization.
- [x] 4.2 Restrict overview to company priorities, company dispatch and personal priorities in descending emphasis; add role-based duties tab between overview and logs using existing dispatch and organization flows.
- [x] 4.3 Verify staff/leader navigation, dispatch, member search and compact calendar on native Android; rebuild APK and update specs/evidence.

## 5. Header, task priority and drag order
- [x] 5.1 Apply consistent safe-area header breathing room and shared task priority labels/order across work pages.
- [x] 5.2 Implement personal-item long-press drag reorder with edge scroll, commit/cancel and persistence; keep company content read-only and document reserved Web-management contract.
- [x] 5.3 Verify ordering boundaries, native drag 10-to-9, persistence, header alignment and light/dark colors; build and package APK.

Latest refinement: compact priority cards (12dp padding, 8dp gaps, one-line summary), remove up/down buttons and their unused ViewModel method. Native drag recognition belongs to the list so lazy item reuse does not cancel cross-row gestures.

## 6. Audible button feedback
- [x] 6.1 Improve click sample gain and media-volume routing, use touch haptic feedback and wire text buttons consistently; add an explicit feedback audition.
- [x] 6.2 Build, test, inspect settings on native Android and package the feedback fix.

White-panel follow-up: unify light-theme black outlines for cards, surfaces, calendar controls/agenda, secondary header controls, outlined inputs and dialogs; native page screenshots reviewed.

## 7. Calendar period views
- [x] 7.1 Remove refresh and calendar navigation borders; expose month/week/day modes, full-week grouped agenda and active-period navigation.
- [x] 7.2 Verify calendar period boundaries and native mode/day/week navigation; package APK and update documentation.

## 8. Native task progress graph
- [x] 8.1 Add a Simple Graph-inspired Compose curve to task details, using actual current workflow state and existing theme tokens without fabricated history.
- [x] 8.2 Verify all three states, small-screen/dark/reduced-motion display and progress feedback; build, package and document the Android deliverable.

## 9. Company depth cards
- [x] 9.1 Adapt native depth cards for two side-by-side overview previews and the complete company collection; preserve read-only details and local remarks.
- [x] 9.2 Verify ten/one/zero items, detail navigation, touch scroll, small width, themes and reduced motion; package the Android build and evidence.

## 10. Teammate sync and local company examples
- [x] 10.1 Fetch and merge teammate zayin, preserve local UI/database compatibility and inspect Web/API for published examples.
- [x] 10.2 Add ten explicit local examples with Debug development entry and opt-in empty-panel preview; prevent example writes.
- [x] 10.3 Verify Android/Web/backend and native examples navigation, package APK and record evidence.

## 11. Consecutive multi-day logs
- [x] 11.1 Add compatible start/end persistence, range coverage queries and server/client bounds and modification protection.
- [x] 11.2 Add a native continuous date picker, date labels and linked month/week frames with one shared weekly description; leave year controls unchanged.
- [x] 11.3 Verify migration, range boundaries, authorization, persistence, native interactions and small-screen themes; deliver Android and API packages.

## 12. Task workspace and account update (supersedes earlier calendar/log editing rules)
- [x] 12.1 Separate the task calendar from logs, include all authorized tasks and allow all active business assignees.
- [x] 12.2 Add manager-reviewed creation/edit/progress, founder exemption/deletion, withdrawal, audit records and persistent app notifications.
- [x] 12.3 Add founder-maintained brand labels selectable by all members and consistent task/calendar colors.
- [x] 12.4 Enforce rolling 24-hour log edits, add avatar/password self-service and remove the progress graph.
- [x] 12.5 Recompose overview as the requested two-by-two grid using existing design tokens.
- [x] 12.6 Complete final native QA and deliver the APK/API pair.
