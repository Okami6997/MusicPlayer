# Changelog

All notable changes to **Music Player** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Added
- Initial project scaffold with Gradle Kotlin DSL multi-module setup (`app`, `wear`, `tv`, `auto`)
- Material 3 / Jetpack Compose UI for phone/tablet
  - Home screen with "Recently Added" list
  - Library screen with Tracks / Albums / Artists / Playlists tabs
  - Full-screen Now Playing (Player) screen with seek slider and transport controls
  - Search screen with real-time debounced search
  - Settings screen (dynamic color, gapless playback toggles)
  - Music Sources management screen (add / delete NAS & local sources)
- **Media back-ends**
  - Local MediaStore scanner (all audio files on device)
  - Subsonic / OpenSubsonic / Navidrome client (token-based MD5 auth)
  - Jellyfin client (MediaBrowser API, token auth)
  - Emby client (same API surface as Jellyfin)
  - Plex (metadata groundwork; stream URL generation)
  - Audiobookshelf (planned)
  - Cloud Drive (planned)
- **Playback**
  - Media3 (ExoPlayer) foreground service with MediaSession
  - Chromecast support via `media3-cast` + Cast Framework
  - DLNA / UPnP groundwork (multicast permission, planned renderer)
  - Sonos groundwork (planned via UPnP AV)
  - Transparent ExoPlayer ↔ CastPlayer switch when cast session starts/stops
- **Offline**
  - Room database caches entire library (tracks, sources, playlists)
  - `markTrackDownloaded()` API; download flow planned for WorkManager
- **Android Auto** — CarApp Media template (browse Library / Recently Played)
- **Wear OS** — Compact now-playing card with Play/Pause, Prev, Next
- **Android TV** — Leanback-compatible TV Material browse screen
- GitHub Actions CI
  - Parallel jobs: phone debug/release APK, Wear APK, TV APK, Auto APK, unit tests, release AAB (main branch only)
  - Gradle build cache & configuration cache enabled
- `ISSUES.md` tracking known limitations and planned work
- `CODEBASE-KNOWLEDGE.md` architectural reference

---

## [0.0.2] - 2026-04-22

### Added
- Updated `app` playback service notification handling to use Media3 `DefaultMediaNotificationProvider` with explicit channel creation and startup foreground notification for more reliable visibility when other media players are active.
- Bumped `app` version to `0.0.2` and `versionCode` to `2`.

---
