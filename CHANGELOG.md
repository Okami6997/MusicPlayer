# Changelog

All notable changes to **Music Player** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

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
