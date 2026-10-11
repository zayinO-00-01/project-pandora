# Pandora Product Design System

## Product Character

Pandora is an enterprise collaboration and productivity app. It should feel professional, efficient, calm, reliable, structured, information-dense, and restrained.

The product is not a marketing landing page, AI dashboard, social app, game, or glassmorphic showcase.

## Tokens

### Color

- Background: `#F6F8FA`
- Surface: `#FFFFFF`
- Surface muted: `#F4F6F8`
- Primary: `#2E61DB`
- Primary soft: `#E8F2FF`
- Text primary: `#1C2430`
- Text secondary: `#667385`
- Border: `#E5E9EF`
- Success: `#1A8C63`
- Warning: `#DE6E1F`
- Error: `#C84646`
- Info: `#435CD5`

### Spacing

Use the 4/8 system: `4, 8, 12, 16, 20, 24, 32, 40, 48`.

### Typography

- Small: 12sp / 18sp
- Secondary: 13sp / 20sp
- Body: 14sp / 22sp
- Body large: 16sp / 24sp
- Section title: 18sp / 26sp
- Page title: 20sp / 28sp
- Display only when necessary: 24sp / 32sp

Use normal and medium weights. Bold is reserved for page titles, section titles, and key values.

### Radius and elevation

- Input and compact control: 6dp
- List row and small module: 8dp
- Dialog or prominent module: 12dp
- Elevation is reserved for dialogs, menus, and floating actions.
- Prefer borders, dividers, spacing, and tonal surfaces over shadows.

## Layout

The mobile shell is stable across screens:

```text
system safe area
page toolbar
scrollable workspace
bottom navigation
```

Desktop layouts, when implemented, use:

```text
global sidebar | optional secondary navigation | main workspace
```

Repeated operational data uses lists and tables before cards. Cards are reserved for independent modules, special states, and primary actions.

## Component rules

Every shared component must define default, pressed, focused, disabled, loading, empty, and error states where applicable. Status must use at least two cues: label plus color, icon plus label, or fill plus position.

## Product modules

- Workbench: current work, due soon, next schedule, unread notices.
- Tasks: searchable, filterable list with owner, status, deadline, progress, and detail actions.
- Calendar: day/week/month views with task-linked agenda rows.
- Journal: structured daily entry connected to tasks.
- Notifications: unread state and direct action routing.
- Analytics: compact metrics and traceable source rows.
