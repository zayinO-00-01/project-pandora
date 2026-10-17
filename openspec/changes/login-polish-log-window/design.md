# Design

## Context
Existing logs support arbitrary past dates and author edits. Android opens all records in its editor; existing transport retains errors and local buffers. See proposal.md for requested changes.

## Goals / Non-Goals
Enforce a single inclusive Shanghai-calendar rule at every write boundary while leaving authorized reading and existing organization scope intact. No database migration, quota, deletion policy or role expansion.

## Decisions
- Compute today in Asia/Shanghai. Validate original and requested dates independently for updates; expired originals return 403 and invalid target/create dates return 400. Submission changes status and therefore observes the same window. Remove server-default-timezone future validation in favor of this explicit rule.
- Android keeps the full record visible in its existing editor with disabled fields and a return action when the original date expires. Recheck original/current dates before save and submit, including a restored buffer and midnight rollover. Date filters remain unrestricted.
- Keep the existing autoplay painting and fade duration; change reveal delay to one second. Remove informational prose and title banners from login, retaining only functional labels, controls and error messages. Collapse connection settings by default.
- Reuse the existing header action layout with a common 36dp visible height inside a 48dp touch target. Refresh uses the same layout and width as write with its own colors/loading state.

## Risks / Trade-offs
- Device date can differ from the server: frontend gives guidance, server remains authoritative and rejects stale writes without discarding input.
- Existing clients may offer expired edits: API returns a readable 403; current Android/Web are updated to render history read-only.

## Migration Plan
Deploy the new backend together with the client; existing data is preserved and requires no migration. Reverting backend restores the earlier unrestricted write behavior.

## User follow-up
Use offline motion interpolation and efficient H.264 predictive encoding for continuous 60 fps playback, preserving the opening/final painting and duration. Render on a decoder surface instead of copying a transparent TextureView each frame. Center the complete login group within available height, and keep it scrollable with keyboard/short screens. Replace header text with plus/refresh icons in matching 36dp squares inside 48dp accessible touch targets; keep current colors and material. Validate timestamps, frame continuity, native presentation timings, centering and lifecycle recreation.

The user added a request for an artistic 潘多拉 title. Use a generated transparent, legible semi-cursive blue-green ink wordmark above the form, fading with it; center the entire group. Do not restore explanatory text. Preload the wordmark before video startup rather than decode it during the fade.

Store active generated painting/wordmark in drawable-nodpi so Android does not enlarge them by device density before compositing (original sizes 1086×1448 and 2164×727). Render at layout size using the original pixels.

## Detailed birds follow-up
Use the generated transparent heron anatomy atlas as the real video texture, not merely a reference. Project articulated shoulder/elbow/wrist wing meshes with texture-preserving triangle warps; the painted body remains stable. Downstroke is slightly faster than recovery; recovery folds and sweeps the outer wing, with passive tip flex and a smoothly blended brief glide. Use one continuous flock traversal in the upper sky with depth-dependent scale/opacity and staggered wing phases. Rebuild from the original clean paintings so the old M-shaped glyphs are completely removed. Offline render at 60 fps with supersampled silhouettes and the existing mobile-efficient H.264 delivery. Preserve the login title/layout and player lifecycle fix.
