# Changelog

All notable changes to **Music Player** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [beta-5] - 2026-06-14

### Known limitations
- Full sync may complete immediately and appear similar to delta sync behavior for some remote providers/sessions. Tracked in `ISSUES.md` while investigation continues.

### Added
- **Delta sync** for all remote sources (Jellyfin, Emby, Plex, Subsonic, OpenSubsonic, Navidrome). Only the tracks that changed since the last sync are downloaded, instead of rebuilding the whole library every time. The library/server "Sync" action is now delta by default.
- Per-source and per-profile **last sync timestamps** (`lastDeltaSyncAt` / `lastFullSyncAt`) persisted in Room and shown on source/profile list items.
- New **Sync** settings screen (reachable from Settings → Sync) with "Sync All Sources (Delta)" and "Full Sync All Sources" actions, plus per-source delta/full sync buttons.
- New `DeltaSyncWorker` and `DeltaProfileSyncWorker` (WorkManager-based) for background delta syncing, matching the existing full-sync workers.
- `Track.remoteUpdatedAt`, `TrackEntity.remoteUpdatedAt`, and `ProfileTrackEntity.remoteUpdatedAt` columns store the server-side "last modified" timestamp for each track. Used by the delta algorithm to skip unchanged tracks.
- Subsonic / OpenSubsonic use the server's cheap `getIndexes.lastModified` check to short-circuit delta syncs when the library hasn't changed.
- Jellyfin / Emby delta sync uses `getItems` with `SortBy=DateModified,SortOrder=Descending` and stops paginating once it reaches the cutoff.
- Plex delta sync fetches the most recent pages of the music library section and filters by `updatedAt` per item.
- Navidrome (native API) delta sync uses `getSongs` with `updatedAt` filtering.
- Global `LAST_DELTA_SYNC_TIME` and `LAST_FULL_SYNC_TIME` DataStore keys for the "Sync" screen.

### Changed
- Default sync behaviour on `SourcesScreen` and `ProfileListScreen` is now **delta sync** (fast, fetches only changes). A new dropdown menu on the per-source/per-profile sync icon exposes a separate **Full Sync** action for when a complete rebuild is required.
- `MusicRepository.syncSource()` and `MusicRepository.fetchTracksFromSource()` remain as the **full sync** entry points (used internally by delta sync as a fallback and exposed by the explicit Full Sync action).
- `MediaSource` and `Profile` domain models now expose `lastDeltaSyncAt` and `lastFullSyncAt` fields.
- Bumped database version 7 → 8 with a `MIGRATION_7_8` that adds the new columns. The migration preserves existing tracks and sources.

### Fixed
- Jellyfin delta sync no longer fetches every item on every run — it stops paginating as soon as it hits the cutoff timestamp.

### Added
- Added background sync for both old UI (Sources screen) and new UI (Profile list and Library screens) using WorkManager. Syncing now runs as a background process that continues even when navigating away.
- Added search functionality to the Library screen - tap the search icon in the top bar to filter tracks, albums, and artists.
- Added `ProfileSyncWorker` for background profile syncing in the new UI.

### Fixed
- Fixed lyrics not loading for profile-based tracks in the new UI. Added `sourceType` field to `ProfileTrackEntity` and updated `LyricsLoader` to look up profiles when track source is not found in MediaSource table.
- Fixed lyrics highlighting regression - now immediately calculates current line index when lyrics are loaded.
- Fixed lyrics overflow in word-by-word sync mode - now uses AnnotatedString for proper text wrapping, preventing lyrics from going outside the screen when lines are long.
- Fixed download URL 404 error - DownloadClient now properly handles URLs with/without `/api/` suffix.
- Fixed search functionality in new UI - now uses ProfileMusicRepository when a profile is selected.
- Fixed Recently Added not showing tracks in old UI - HomeViewModel now uses profile-based tracks when available.
- Incremented database version to 7 for schema changes (ProfileTrackEntity sourceType column).

### Changed
- Updated database version from 6 to 7 for ProfileTrackEntity schema changes.
- Updated `ProfileMusicRepository.saveTracks()` to accept sourceType parameter and properly store the source type with each track.
- Updated `ProfileTrackEntity` to include `sourceType` field for tracking the original media source type (SUBSONIC, NAVIDROME, JELLYFIN, etc.).

---

## [beta-3] - 2026-05-11

### Added
- Added background sync for media sources using WorkManager. Syncing a source now runs as a background process that continues even when navigating away from the Sources screen.

---

## [beta-2] - 2026-05-10

### Added
- Added `OldUiLibraryViewModel` — a dedicated ViewModel for the old UI library that always reads from `MusicRepository` directly, ensuring old UI library data is fully isolated from New UI profile-based data.
- Added `NewUiSettingsScreen` for the New UI, with profile-related settings for Profiles, Appearance, Audio, Offline, Dynamic Color, and Gapless Playback.
- Added `ProfileMusicRepository` to support profile-isolated music data access using the `ProfileTrackEntity` table.

### Changed
- Updated `DownloadClient.kt` to append `/api/` to base URLs when constructing download service URLs, fixing the 404 error for downloads.
- Modified `NewUiHomeScreen` to add a `MiniPlayer` component when a track is playing and expose an `onNavigateToPlayer` callback.
- Updated `NewUiNavGraph.kt` to use the new settings screen and pass the player navigation callback.
- Updated `NavGraph.kt` to use `OldUiLibraryViewModel` for the old UI library.
- Updated `LibraryScreen.kt` to accept a `libraryViewModel` parameter instead of a generic `viewModel`.
- Fixed an extra closing brace syntax error in `NewUiHomeScreen.kt`.

---

## [beta-1] - 2026-05-09

### Added
- Added a dedicated Download Source flow from Settings to configure and validate a remote download URL.

### Changed
- Simplified offline labeling from `Offline / Downloads` to `Offline` in relevant UI surfaces.
- Updated navigation to include a Download Source destination and callback wiring from Settings.
- Added DataStore-backed persistence for the download source URL and URL reachability test state.

---

## [0.0.8] - 2026-05-01

### Changed
- Updated `app` version to `0.0.8` and `versionCode` to `8`.
- Polished navigation and UI interactions in Home, Library, Search, and Settings screens.

---

## [0.0.7] - 2026-04-25

### Added
- Polished UI for a minimalist yet modern look and feel — refined color schemes, typography hierarchy, TrackListItem, MiniPlayer, empty states, and all major screens.
- Updated `app` version to `0.0.7` and `versionCode` to `7`.

---

## [0.0.6] - 2026-04-25

### Added
- Added Android Auto settings and improved theme/appearance persistence across launches.
- Added `Add to Playlist` and playlist detail workflows to simplify queue management.
- Removed equalizer feature — not working on all devices.
- Updated `app` version to `0.0.6` and `versionCode` to `6`.

---

## [0.0.5] - 2026-04-25

### Added
- Added equalizer support and audio settings screens for customizable playback.
- Added Android Auto settings and improved theme/appearance persistence across launches.
- Added `Add to Playlist` and playlist detail workflows to simplify queue management.
- Updated `app` version to `0.0.5` and `versionCode` to `5`.

---

## [0.0.4] - 2026-04-25

### Added
- Added local playback support with improved album and artist browsing.
- Added playlist detail screens and Add to Playlist dialog for in-app queue management.
- Enhanced media source persistence and player state handling across the app.
- Updated `app` version to `0.0.4` and `versionCode` to `4`.

---

## [0.0.3] - 2026-04-25

### Added
- Improved Media3 notification provider handling and startup foreground service behavior for Samsung notification visibility.
- Bumped `app` version to `0.0.3` and `versionCode` to `3`.

---

## [0.0.2] - 2026-04-22

### Added
- Updated `app` playback service notification handling to use Media3 `DefaultMediaNotificationProvider` with explicit channel creation and startup foreground notification for more reliable visibility when other media players are active.
- Bumped `app` version to `0.0.2` and `versionCode` to `2`.

---
