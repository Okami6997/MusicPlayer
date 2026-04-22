# Known Issues & Planned Work

This document tracks confirmed bugs, missing features, and planned improvements.
For individual bug reports please open a [GitHub Issue](../../issues).

---

## 🐛 Known Bugs

| # | Module | Description | Workaround |
|---|--------|-------------|------------|
| 1 | `app` | MediaStore scan requires READ_MEDIA_AUDIO permission on Android 13+; app does not yet prompt at launch | Grant permission manually in Settings |
| 2 | `app` | Cast player position tracking is not synced back to UI after switching devices | Re-open the Player screen |
| 3 | `app` | Artwork URI from Subsonic is not authenticated — image may not load behind strict firewalls | None currently |
| 4 | `wear` | Wear OS UI is a placeholder stub; playback controls are not connected to the phone session | Use phone app |
| 5 | `tv` | TV module uses placeholder content; library browsing not yet wired to the shared database | Use phone app |
| 6 | `auto` | Android Auto CarApp screen items are static placeholders | Use phone app |
| 7 | `app` | Samsung/media output notification visibility may be inconsistent when other media players are active | Verified improvement in v0.0.2 using Media3 notification provider and startup foreground notification |

---

## 🚧 Missing Features (Planned)

### Core Playback
- [ ] DLNA / UPnP renderer discovery and playback routing
- [ ] Sonos controller via SoCo-style UPnP AV
- [ ] Crossfade between tracks
- [ ] Equalizer / audio effects (via AudioEffect API)
- [ ] Gapless playback (ExoPlayer supports it; needs enabling in pipeline)
- [ ] ReplayGain / volume normalization

### Offline & Downloads
- [ ] WorkManager-based background download queue
- [ ] Configurable download quality per source
- [ ] Automatic sync / delta updates from remote sources
- [ ] Offline mode toggle (disable all network calls)

### Library Management
- [ ] Full Plex Music API integration (token via OAuth PIN flow)
- [ ] Audiobookshelf integration
- [ ] Cloud Drive (Google Drive, Dropbox, OneDrive, WebDAV)
- [ ] Smart playlists / auto-playlists
- [ ] Last.fm scrobbling
- [ ] Lyrics display (LRC / synced)

### UI / UX
- [ ] Animated album art transitions
- [ ] Swipe-to-dismiss mini player
- [ ] Lock screen / notification artwork
- [ ] Widget (home screen)
- [ ] Tablet / landscape split-pane layout
- [ ] Dark / AMOLED theme override independent of system setting

### Platform Modules
- [ ] Wear OS: full MediaSession integration with phone companion
- [ ] Android TV: full D-pad-navigable library browser
- [ ] Android Auto: real-time library browsing from CarApp session

### Infrastructure
- [ ] Signed release APK / AAB via GitHub Actions secrets
- [ ] Automated Play Store upload via Fastlane
- [ ] Instrumented (on-device) CI tests
- [ ] Crashlytics / Firebase integration
- [ ] R8 / ProGuard rule tuning for release build size

---

## 💡 Feature Requests

Open an issue with the label **enhancement** to suggest new features.
Please search existing issues before creating a new one.
