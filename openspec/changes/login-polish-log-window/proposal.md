# Login polish and bounded log writes

## Why
The user wants a faster, quieter login and equally sized header actions. Historical logs must remain readable while writes are limited to seven days ago through today, with server enforcement.

## What Changes
- Begin login form reveal one second after the video starts; remove explanatory/marketing copy and default connection settings to collapsed.
- Use identical 36dp visible square icons and 48dp touch containers for refresh and write actions; retain current materials and accessible names.
- Center the complete login form and optimize continuous 60 fps background playback.
- Replace simple bird glyphs with generated detailed herons and articulated flap/glide animation.
- **BREAKING**: create, update and submit only within the inclusive Shanghai-date range today minus seven days through today. Reject updates to expired originals even when a request moves their date into the window.
- Keep authorized history queries unrestricted by date; display expired records read-only in Android and Web.

## Capabilities
### New Capabilities
None.
### Modified Capabilities
- `log-demo`: server-controlled writing window and unrestricted authorized history reads.
- `android-log-client`: faster minimal login and read-only historical log display.

## Impact
Android login, header actions, log editor and validation; existing backend log endpoints; Web journal actions; OpenAPI; boundary and permissions tests. No new runtime dependencies or database schema changes.
