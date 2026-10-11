# Pandora UI Audit

Date: 2026-10-05
Scope: current native Android prototype before Design System refactor

## Executive Summary

The project is a runnable single-activity prototype, not yet a maintainable enterprise collaboration UI system. Its main risk is systemic inconsistency: screen construction, tokens, components, navigation, data, and interaction state all live in one Java file. Visual adjustments made at page level will continue to create drift until the shared system is established.

## Project Structure

- `MainActivity.java` contains login, app shell, navigation, all screens, demo data, tokens, component helpers, and dialogs.
- There is no `DESIGN.md` or `UI_RULES.md`.
- There is no shared component package for buttons, fields, status badges, lists, dialogs, navigation, or tables.
- There is no data/state layer; tasks and notifications are demo objects created in the Activity.
- The current target is portrait-only Android (`screenOrientation="portrait"`), so the desktop workspace shown in the Figma reference is not represented in the APK shell.

## Findings By Layer

### Design System

1. Tokens are incomplete. Colors exist as Java constants, but typography, spacing, radius, borders, elevation, icon sizes, and states are not tokenized.
2. The code still contains semantic aliases such as `BLUE = PRIMARY` and many direct `Color.rgb(...)` values inside screens.
3. Typography is only `setTextSize()` plus a bold helper. There is no shared type scale, line-height contract, or weight policy.
4. Radius values are mixed across helpers and screens. The visual language alternates between large rounded cards, pills, and ordinary rectangles without a documented component rule.
5. Glass styling is still present even though the new enterprise brief explicitly rejects glassmorphism as a default visual language.

### Components

1. `button()`, `pill()`, `box()`, `glassBox()`, `filledField()`, and `quickRow()` are local helpers rather than reusable component classes with variants.
2. Button states are incomplete: no consistent hover, focus, disabled, loading, or pressed behavior is defined.
3. Inputs lack shared error, disabled, filled, focus, validation, and label states.
4. Status is encoded mostly by color and text; there is no shared status component with icon/label/shape cues.
5. Tables, dense lists, dialogs, dropdowns, avatars, toolbars, breadcrumbs, and empty/error/loading states are not implemented as system components.
6. The onboarding dialog is a bespoke implementation rather than a reusable stepper/dialog pattern.

### Layout / App Shell

1. The app is a single-column portrait shell. It does not have the required stable desktop-first structure of global sidebar, optional secondary navigation, and main workspace.
2. Home and view screens use a special compact shell while other screens use a toolbar shell. This creates page-to-page structural drift.
3. `ScrollView` plus manually assigned minimum heights is used as a general layout strategy. This is fragile for longer Chinese text, accessibility font scaling, and smaller devices.
4. Bottom navigation is a set of text controls rather than an icon-and-label navigation component with selected, pressed, disabled, and badge states.
5. There is no persistent global search, workspace switcher, notification center, user menu, breadcrumb, or desktop context panel.

### Pages / Product Workflows

1. Home is still a dashboard of static modules rather than a configurable workbench.
2. Tasks now have a demo filter, but search does not filter data and task state is not persisted as a real workflow.
3. Calendar is a date grid plus agenda rows, not a true time grid with event blocks, editing, conflicts, or task synchronization.
4. Journal fields are local controls; they are not associated with task IDs, dates, authors, or submission states.
5. AI summary is fixed copy, not a data-backed assistant with traceable sources and actionable states.
6. Notifications are static demo entries; read/unread state and action routing are not persisted.
7. Profile, settings, permissions, audit, and data scope are explanatory rows rather than functional enterprise controls.

## AI Slop / Visual Risk

- Repeated cards are used where lists, dividers, or grouped rows would communicate more efficiently.
- Glass cards, pills, and rounded containers are still more prominent than the enterprise brief allows.
- The product currently mixes Material You, Quiet Glass, and dashboard-card conventions without a declared primary system.
- Fixed demo copy and metrics can look polished but do not communicate real product capability.
- Directly using symbols such as `‹`, `●`, and `○` as icons is not a consistent icon system.

## Visual QA Gaps

- No automated screenshot or device-matrix verification exists.
- No checked viewport matrix for small Android phones, large phones, font scaling, landscape, or accessibility settings.
- No state-by-state QA for buttons, inputs, lists, dialogs, filters, or notifications.
- No real device is currently connected through ADB, so the last APK builds were compile-verified only.

## Recommended Phase 2 Direction

1. Choose a restrained enterprise system: neutral surfaces, blue primary, semantic status colors, 4/8 spacing, 4/6/8/12 radii, minimal elevation, no default glass.
2. Create `DESIGN.md` and `UI_RULES.md` with tokens and anti-AI-slop rules.
3. Extract shared Java components or XML layouts for AppShell, Header, Sidebar, Toolbar, Button, Input, Select, Status, ListRow, Dialog, and Empty/Loading/Error states.
4. Rebuild three golden pages first: Workbench, Task List, and Task/Create Settings.
5. Add a real state model before expanding Calendar, Journal, Notifications, and AI surfaces.
6. Run screenshot-based Visual QA on every golden page before migrating the remaining screens.
# 2026-10-05专项整改审计

## 已定位的裁切风险（整改前）

- `UiComponents.button` 使用 `minHeight` 且垂直 padding 为 0，中文按钮容易上下贴边。
- 工作台筛选按钮放在固定 48dp 高的横向行中，按钮视觉尺寸与搜索框不一致。
- 首页“去记录”使用固定 78x30dp，低于中文按钮安全高度。
- `sectionHeader` 将“查看全部”等文字放进固定宽度 74dp Button，长中文存在横向裁切风险。
- `pill` 使用固定最小高度和较大圆角，状态标签与操作按钮职责混淆。
- 多个任务行使用固定视觉高度，动态标题区域没有统一的最大行数策略。
- 普通按钮统一使用同一背景工厂，但未区分 Primary、Secondary、Filter 的语义。

## 本次处理

- 统一 Button 的中文安全区、水平/垂直 padding、单行省略和 7dp 圆角。
- 新增 `UiComponents.filterButton`，使用实色 Surface、轻边框、无阴影的 Secondary 语义。
- 筛选按钮改为 wrap_content，不再承载完整筛选条件文案。
- Section 操作改为自然宽度文字入口，移除固定 74dp Button。
- 状态标签保留为紧凑 Tag，统一单行省略和 8dp 圆角。
- 首页操作按钮改为内容驱动高度，避免 30dp 固定高度压缩中文字形。

## 后续 Visual QA

- 重点检查首页、工作台、任务、日志、通知、设置中的长中文按钮和标题。
- 重点检查窄屏下 Toolbar 的搜索框与筛选按钮是否保持 40dp 基线。
- 构建通过后仍需在真实设备上以系统字体 110% 和 120% 复测，因为静态编译无法替代 Android 字形渲染检查。
