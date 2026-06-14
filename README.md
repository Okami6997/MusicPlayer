# MusicPlayer
Cross-platform Android music player for phone, Wear OS, TV, and Auto.

## Project modules
- `app` — phone/tablet application
- `wear` — Wear OS companion app
- `tv` — Android TV app
- `auto` — Android Auto Car App

## Current release
- beta-1 — Download Source configuration flow, offline label cleanup, DataStore-backed URL persistence

## Current progress
- Refined color schemes, typography hierarchy, and component styling for a modern minimalist aesthetic.
- Improved empty states, tab row styling, and playback control visuals.
- Added Download Source configuration from Settings with URL persistence and connectivity testing.
- Standardized offline/download wording in UI to improve clarity.
- Added a separate New UI settings screen with profile-related options and a New UI mini-player with direct full-player navigation.
- Strengthened data isolation by separating New UI profile-based data through `ProfileMusicRepository` and `ProfileTrackEntity`, while old UI library data remains on `TrackEntity` via `OldUiLibraryViewModel`.
- Fixed download URL construction in `DownloadClient.kt` to avoid 404 responses.

## Known issues / bugs
- Full sync may complete almost instantly for some remote sources and behave similarly to delta sync in runtime behavior.
- See ISSUES.md for the tracked status and workarounds.

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

