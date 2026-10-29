> Historical implementation record across multiple teammate branches. The 2026-10-29 integration resolves conflicting rules in `openspec/specs/workspace-planning/spec.md`; branch-local verification below is not evidence of integrated-device testing.

## Context
当前工作区已有未提交的界面、组织授权和七天日志规则。保留这些修改、登录水墨背景和开发入口，仅增补本次要求。
## Decisions
十大事沿用原生 Compose。公司内容只读，支持个人备注；个人可新增编辑删除与上下排序，上限 10 条。使用独立 SharedPreferences 按 server/userId 隔离本地快照及备注，显式标记仅本机未同步，接口契约单独记录；不调用不存在的写接口。
月历采用参考图的七列留白布局和日期下短标题，优先任务标题、其次日志首行，每格摘要自动换行且最多两排文字，优先显示完整短标题；所有详情与数量在选中后显示；周日为首列、当日圆形标识、选中格强调，下方展示选中日详情。保留现有主题及字体，不导入图片的手写字体或节假日数据。
姓名搜索只过滤服务器已授权成员，支持姓名、账号、清空与无结果，不改变组织权限。
日报日期为业务日期。创建窗口 [today-3,today]，更新/提交仅 originalDate=today 且更新 targetDate=today。历史补录提交后立即只读；历史补录草稿也不能再改或提交，前端明确提醒。上海时区统一，保存及恢复时重验。同步 Web 与 API 防止绕过。
## Validation
覆盖日期跨年边界、未来/过期创建、旧记录换日期与提交、作者权限；Android JVM、后端 H2 测试、Web 构建。实际模拟器检查事项编辑、月历摘要、员工搜索及补录日期，不伪称真实接口联调。

用户补充：开发直接进入按钮固定在 Debug 登录页右上角，不等待动画、不随表单滚出屏幕。Release 隐藏，预览业务请求保护保留。

## Overview/duties refinement
Use existing native components, dispatch form and server authorization. Overview has exactly three content sections, company priorities min 248dp (two side-by-side depth card previews), company dispatch min 168dp (2 previews), personal priorities min 112dp (2 compact previews); sections grow with content and scroll naturally. No log, member entry, or pending-work module on overview. Functions sits between overview/logs in five-tab navigation. Leaders get a dominant dispatch area and authored-task previews, plus a compact organization entry; staff get their own pending tasks and a compact manager entry. Task list, task detail, dispatch and organization are duties subroutes. Back returns to duties; draft buffers remain intact. Existing API writes and permission checks are reused.
Priority pages have no search. Member list has persistent local name/account query, clear/count/no-match states inside organization. Calendar rows decrease from 98dp to 72dp, number from 34dp to 30dp and summary block from 40dp to 28dp, still max two text lines and accessible day touch area >=48dp. Verify 320dp and normal width without truncating navigation labels.

## Header/priority/drag refinement
Authenticated page headers share safeDrawing top inset plus 12dp breathing room and 6dp bottom spacing; title and actions move together. Do not double-count TopAppBar insets or change the video login placement.
Tasks share an unfinished-first comparator, then high/normal/low rank, then parsed Instant deadline ascending (missing/invalid last), then ID for stable ties. High uses accessible red, normal teal, low muted gray, with separate text labels; adapt to dark theme. Overview dispatch, duties, full task list, member task list and calendar tasks reuse the same order.
Personal items use native long-press drag with a visible handle, elevated dragged card, in-memory reorder, edge scrolling and commit on release. Gesture cancellation restores the original order. Persist the final per-account snapshot only after drop; use compact 12dp card padding, a single-line detail preview and 8dp list gaps; remove visible up/down controls per the latest user request. Company items remain Android read-only; reserve Web administrative write contract only, without implementing backend editing.

## Feedback and borders refinement
Route the optional short click through media volume, retain silent/vibrate gating, raise sample gain 2x without clipping and playback gain from 0.22 to 0.65. Use VIRTUAL_KEY touch feedback instead of keyboard-specific feedback, preserving system settings. Reuse ControlTextButton for textual actions without duplicate calendar calls. Profile offers enabled-feedback audition and volume/system guidance. Light theme uses black outline tokens and thin 1dp borders on white surfaces, calendar boxes, secondary header buttons and dialogs; dark surfaces retain their existing colors.

## Calendar modes refinement
Reuse existing selectedDate/calendarMode state and periodDates. Month remains Sunday-first; week uses the existing Monday-first helper and labels Monday through Sunday. Weekly agenda groups all seven days and exposes a day-detail action, compact empty states and existing task ordering. Day omits the grid and shows the selected date agenda only. Navigation shifts by current period. Mode changes retain date and reset list scroll. Remove explicit borders from refresh and the three period controls, preserving content panel borders.

## Native task progress graph
The user explicitly selects native Android task details, overriding the original phase-one chart exclusion. Interpret Simple Graph's smooth curve, line reveal, subtle gradient and dot emphasis using Compose Canvas/Animatable and existing teal/copper tokens. No React registry, license secret or React dependency is needed for the selected native implementation. The three evenly spaced positions are qualitative workflow stages (todo/doing/done), never percentages or historical elapsed time. The API only supplies current status/latest note and creation/update timestamps; display those without inventing milestone dates. Reveal once in 900ms, respect Android animation duration scale, provide a semantic description, adapt to both themes and keep the existing authorized feedback workflow.

## Company depth cards
Follow the user's native Android choice when adapting Depth Card. Reuse one Compose card for the first two sorted company items in overview and the full two-column list under View all. A single item retains one column and empty data shows the existing empty/error states. Card selection opens the existing read-only detail and local remark editor. Use paper/teal/copper theme tokens, black light-mode outlines, offset backing and a restrained 4-degree pointer tilt/press lift. Observe pointer events without consuming vertical scroll; disable transforms when system animators are disabled. No React dependencies, registries, paid source or key are introduced.

## Explicit local company examples
After fetching zayin f91516e, Web remains an account console and companyImportant remains empty. Supply ten local examples with negative IDs, explicit labels and no write controls. Debug users opt in via development entry or an empty authenticated company panel, then can return to actual server data. Never replace live server data silently. Preserve the local database migration order during teammate integration; keep business data untouched.

## Consecutive multi-day logs
The user keeps the existing today-minus-three-days window and assigns year selection to a teammate; leave year navigation untouched. Add optional endDate, defaulting to logDate for legacy data, and V7 migration to backfill. Query ranges inclusively for date-filtered reads and today's panel. Only today-to-today records may be modified/submitted after creation; multi-day records must be submitted directly. Two compact native date-button rows expose all four allowed days and select one interval, stored in the existing per-server/author draft. A capability flag prevents posting multi-day data to older servers. Calendar draws visible-week portions of each range and deduplicates shared details in weekly mode; single-day reads and calendar summaries use inclusive coverage. No new dependencies.
