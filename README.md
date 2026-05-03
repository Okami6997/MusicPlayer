# MusicPlayer
Cross-platform Android music player for phone, Wear OS, TV, and Auto.

## Project modules
- `app` — phone/tablet application
- `wear` — Wear OS companion app
- `tv` — Android TV app
- `auto` — Android Auto Car App

## Current release
- v0.0.8 — incremental UI improvements, navigation polish, and bug fixes across key screens

## Current progress
- Refined color schemes, typography hierarchy, and component styling for a modern minimalist aesthetic.
- Improved empty states, tab row styling, and playback control visuals.
- Added Download Source configuration from Settings with URL persistence and connectivity testing.
- Standardized offline/download wording in UI to improve clarity.

## Build
```bash
./gradlew :app:assembleDebug
./gradlew :wear:assembleDebug
./gradlew :tv:assembleDebug
./gradlew :auto:assembleDebug
./gradlew testDebugUnitTest
```

## Contributing
Please use `ISSUES.md` to review open issues and planned work before submitting new bug reports or feature requests.

