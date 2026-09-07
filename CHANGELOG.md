# Changelog

All notable changes to the "Work Log" (บันทึกงานช่าง) application will be documented in this file.

## [v1.1.0] - 2026-07-24

### Added
- **Automatic Google Sheet Sync**: Added background coroutine sync utilizing `OkHttpClient` to automatically post newly recorded data, solutions, and task details to any Google Apps Script Webhook.
- **Google Sheets Settings UI**: Added Webhook URL configuration field in the Settings screen so technicians can easily input their target Google Apps Script URL.
- **Calendar UI Summary View**: Designed a polished, modern calendar UI on the Report/Summary screen with subtle blue dot indicators highlighting dates with recorded tasks. Tapping on any calendar date displays a detailed bottom dialog summarizing that day's logs, categories, technician names, and outcomes.
- **Daily Quick Summary**: Replaced the long, redundant summary template with a modern, high-density **Daily Quick Summary Card** that tracks today's total cases, closed vs open cases, and displays a bulleted, easy-to-read checklist of key findings.

### Changed
- **Today-Only Task List Filter**: Updated the local query flow in `WorkLogViewModel` and `LogListScreen` to strictly display cases for the current day only. Past tasks are automatically hidden from the main feed to help technicians maintain razor-sharp focus on active today-only assignments.
- **UI & Aesthetics**: Refined the spacing, typography (using the beautiful Sarabun font), and layout alignments across the app, ensuring adherence to standard Material Design 3 guidelines.
- **App Metadata**: Incremented target app version code to `3` and updated version name to `"1.1.0"`.
