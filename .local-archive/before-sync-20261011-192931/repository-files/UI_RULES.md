# Pandora UI Rules

## Never introduce without a specific product reason

- Purple or blue gradients
- Glassmorphism as a default surface
- Large decorative shadows
- Giant headings
- Excessive whitespace
- Every section as a card
- Excessive pill controls
- Decorative illustrations or floating ornaments
- Continuous animation
- Color-only state indicators

## Prefer

- Stable 4/8 spacing
- Dividers and grouped rows
- Toolbars with search and filters
- Dense but readable lists
- Semantic colors only for status
- One primary action per page
- Explicit empty, loading, error, disabled, and success states
- Reusable components and tokens

## Before changing a screen

1. Read `DESIGN.md`.
2. Inspect existing components and reuse equivalents.
3. Keep the existing mobile shell and navigation.
4. Use tokens instead of page-specific colors or spacing.
5. Check text wrapping and system safe areas.
6. Validate default, pressed, disabled, empty, and error states.

## Visual QA checklist

- No overlap or clipping at small phone widths.
- Toolbar, list columns, fields, and buttons share a baseline.
- Long Chinese text wraps without changing the intended hierarchy.
- Primary action is obvious without making every control prominent.
- Repeated data is presented as rows or tables, not a wall of cards.
- Navigation is consistent across every screen.
- No screen depends on a decorative effect to communicate meaning.
