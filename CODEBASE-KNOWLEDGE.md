# Codebase Knowledge

A concise reference guide to the **Music Player** Android project for new contributors and AI assistants.

---

## Project Layout

```
MusicPlayer/
├── app/                        # Phone / tablet application (main module)
├── wear/                       # Wear OS companion application
├── tv/                         # Android TV application
├── auto/                       # Android Auto (CarApp) application
├── gradle/
│   ├── libs.versions.toml      # Single source of truth for all dependency versions
│   └── wrapper/
├── .github/workflows/build.yml # CI: builds all modules in parallel
├── CHANGELOG.md
├── ISSUES.md
└── CODEBASE-KNOWLEDGE.md       # ← you are here
```

---

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Language | **Kotlin** (100%) |
| UI (phone/tablet) | **Jetpack Compose** + **Material 3** |
| UI (Wear OS) | **Wear Compose Material** |
| UI (TV) | **TV Compose Material** |
| UI (Auto) | **AndroidX CarApp** (template-based) |
| Dependency Injection | **Hilt** (Dagger) |
| Async | **Kotlin Coroutines** + **Flow** |
| Persistence | **Room** (SQLite) |
| Preferences | **DataStore** (Preferences) |
| Network | **Retrofit** + **OkHttp** |
| Image loading | **Coil** |
| Playback engine | **Media3 / ExoPlayer** |
| Cast | **Media3 Cast** + **Google Cast Framework** |
| Build system | **Gradle (Kotlin DSL)** + version catalog |
| CI | **GitHub Actions** |

---

## Architecture

The project follows **MVVM + Clean Architecture** with three layers:

```
UI Layer          →  ViewModel  →  Repository  →  Data Sources
(Compose screens)    (StateFlow)   (single truth)  (Room / Network)
```

### `app` module packages

| Package | Purpose |
|---------|---------|
| `domain/model` | Pure Kotlin data classes (Track, Album, Artist, Playlist, MediaSource, PlayerState) |
| `data/local` | Room entities, DAOs, MusicDatabase, LocalMediaScanner |
| `data/remote/subsonic` | Retrofit API + models + client for Subsonic / OpenSubsonic / Navidrome |
| `data/remote/jellyfin` | Retrofit client for Jellyfin and Emby |
| `data/repository` | MusicRepository — aggregates all sources, persists to Room |
| `service` | MusicPlaybackService (MediaSessionService), PlayerHolder (ExoPlayer ↔ CastPlayer switch), custom MediaStyle notification provider for Samsung media output behavior |
| `cast` | CastOptionsProvider — configures Google Cast receiver |
| `di` | Hilt modules: AppModule (DB, OkHttp), DataStoreModule |
| `ui/navigation` | NavGraph, Screen sealed class |
| `ui/home` | HomeScreen + HomeViewModel |
| `ui/library` | LibraryScreen + LibraryViewModel (groups tracks into albums/artists) |
| `ui/player` | PlayerScreen + PlayerViewModel |
| `ui/search` | SearchScreen + SearchViewModel (debounced, flatMapLatest) |
| `ui/settings` | SettingsScreen + SettingsViewModel (DataStore) |
| `ui/sources` | SourcesScreen + SourcesViewModel (CRUD for MediaSource) |
| `ui/components` | Reusable composables: TrackListItem, MiniPlayer |
| `ui/theme` | Material 3 Theme, Typography, dynamic color support |

---

## Key Data Flow

### Local music scan
```
User taps "Scan Local Library"
  → HomeViewModel.refreshLocalLibrary()
  → MusicRepository.scanLocalLibrary()
  → LocalMediaScanner.scan()        ← queries MediaStore
  → TrackDao.upsertTracks()         ← persists to Room
  → StateFlow emits                 ← UI re-renders automatically
```

### Adding a remote source (e.g. Navidrome)
```
User fills AddSourceDialog
  → SourcesViewModel.addSource(MediaSource)
  → MusicRepository.saveSource()
  → MediaSourceDao.upsertSource()
```

### Playback
```
User taps a track
  → PlayerViewModel.playTracks(tracks, startIndex)
  → PlayerHolder.currentPlayer.setMediaItems(...)
  → ExoPlayer starts buffering and plays
  → MusicPlaybackService holds MediaSession
  → System notification + lock screen controls appear
```

### Cast switching
```
User connects Chromecast via system Cast button
  → CastPlayer.SessionAvailabilityListener.onCastSessionAvailable()
  → PlayerHolder.switchToPlayer(castPlayer)
  → current queue/position transferred to CastPlayer
  → media streams directly from source URL to Chromecast
```

---

## Adding a New Media Source Backend

1. Create `data/remote/<backend>/` package
2. Define Retrofit API interface and response models
3. Create a `*Client` class with helper methods (token, stream URL, cover art URL)
4. Add a new `MediaSourceType` enum entry in `domain/model/MediaSource.kt`
5. Wire the client into `MusicRepository` (call it when syncing a source of that type)
6. Add an icon/branch in `SourcesScreen.kt` -> `SourceListItem`

---

## Running the App

### Prerequisites
- Android Studio Ladybug (2024.2) or later
- JDK 17
- Android SDK 35

### Build
```bash
./gradlew :app:assembleDebug      # phone debug APK
./gradlew :wear:assembleDebug     # Wear OS debug APK
./gradlew :tv:assembleDebug       # TV debug APK
./gradlew :auto:assembleDebug     # Auto debug APK
./gradlew testDebugUnitTest       # run unit tests
```

### Install on device
```bash
adb install -r app/build/outputs/apk/phone/debug/app-phone-debug.apk
```

---

## GitHub Actions CI

The workflow (`.github/workflows/build.yml`) runs four parallel build jobs plus a test job on every push and PR:

| Job | Trigger | Output |
|-----|---------|--------|
| `build-phone` | all branches | phone debug + release APK artifacts |
| `build-wear` | all branches | wear debug APK artifact |
| `build-tv` | all branches | tv debug APK artifact |
| `build-auto` | all branches | auto debug APK artifact |
| `test` | all branches | JUnit test reports |
| `build-release-bundle` | push to `main` only | phone release AAB artifact |

Gradle build cache is enabled via `gradle/actions/setup-gradle@v3` with an optional `GRADLE_ENCRYPTION_KEY` secret for remote cache encryption.

---

## Dependency Versions

All versions live in `gradle/libs.versions.toml`.  
To update a dependency: change the version there and Gradle will pick it up automatically.

Key versions (as of initial scaffold):

| Library | Version |
|---------|---------|
| AGP | 8.5.2 |
| Kotlin | 2.0.21 |
| Compose BOM | 2024.10.00 |
| Media3 | 1.4.1 |
| Hilt | 2.52 |
| Room | 2.6.1 |
| Retrofit | 2.11.0 |
| Cast Framework | 21.5.0 |
| Wear Compose | 1.3.1 |
| TV Compose | 1.0.0-alpha11 |
| CarApp | 1.7.0 |
