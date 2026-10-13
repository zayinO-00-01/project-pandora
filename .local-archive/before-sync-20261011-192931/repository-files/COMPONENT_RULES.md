# Component Rules

## Shared primitives

Use the smallest stable set of primitives: `Button`, `Text`, `Input`, `Status`, `ListRow`, `Toolbar`, `Dialog`, `Navigation`, and `Surface`.

Do not create page-specific variants named `Hero`, `Insight`, `MetricCard`, `QuickCard`, or `GlassBox` when the semantics are a normal section, list row, status, or surface.

## States

- Button: default, pressed, focused, disabled, loading.
- Input: default, focused, filled, error, disabled.
- List row: default, pressed, selected, completed, overdue.
- Surface: default, selected, warning, error, empty.

## Density

Operational rows use 8dp vertical rhythm and compact metadata. Independent business modules may use 12dp padding. Do not increase whitespace to make a page feel more premium.

## Cards

Use a bordered surface only when the content has independent business meaning or needs an explicit boundary. Use dividers and grouped rows for ordinary operational data.
