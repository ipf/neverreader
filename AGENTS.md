# AGENTS.md

Guidance for AI agents (and humans) working in this repository.

## What this is

**NeverReader** — an Android read-it-later app that is a frontend for self-hosted,
non-commercial backends: **Readeck** and **Wallabag** (v1). It is a fork of the
archived Pocket Android app; the Pocket backend and all tracking/analytics have been
removed. There must be **no tracking, analytics, crash reporting, ads, or telemetry**
of any kind. Do not re-introduce: Snowplow, AppCenter, Adjust, Braze, Firebase,
Sentry, or any similar SDK.

## Build & test

Requires **JDK 21** (Gradle 8.x cannot run on Java 25) and the Android SDK
(`ANDROID_HOME` or `local.properties`). `targetSdk` is 35, `minSdk` 26.

```bash
export JAVA_HOME=/usr/lib/jvm/java-1.21.0-openjdk-amd64
./gradlew :app:assembleDebug :app:testDebugUnitTest :backend:test :utils-android:testReleaseUnitTest :app:lintDebug
```

Run it without `--offline`: the lower viewBinding artifacts the app module
resolves are not in the local cache.

## Modules

- [`app/`](app) — the Android app (UI, previously `Pocket/`).
- [`backend/`](backend) — backend abstraction: domain model, Room database,
  `Backend` interface, Readeck + Wallabag adapters, sync, account management.
- [`ui/`](ui) — the Compose design system and shared components (`ui/theme/` for
  colour, type, shape and `AppTheme`; `ui/compose/` for `AppBar`, `ItemRow`,
  `FilterChips`, `SettingsList`).
- [`utils/`](utils), [`utils-android/`](utils-android) — shared utilities.

## Architecture

```
app (UI) → repositories → Room DB → WorkManager sync
                              ↑
            :backend — Backend interface
                ├─ ReadeckAdapter   (OpenAPI spec: api.json)
                └─ WallabagAdapter  (REST v2/v3)
```

- **Domain model:** `Bookmark`, `Tag`, `Annotation`, `Account`. All backends map
  their entries into these types. UI code must only use the domain types, never
  backend-specific DTOs.
- **UI:** Jetpack Compose with Material 3, and Compose only — there are no
  fragments and no XML layout for screen content, and none should be added. Each
  activity extends `AbsNeverReaderActivity` and calls `setAppContent { … }`.
  Navigation is Compose Navigation: routes are declared in `app/…/AppNavHost.kt`
  (no `res/navigation` graph), destinations are plain `@Composable` screens named
  `*Screen.kt`, and each gets its ViewModel from `hiltViewModel()`. Add a screen
  as a `composable(route) { … }` entry in `AppNavHost`, not as an activity.
  Note `setAppContent` may be called again (e.g. to swap content), which
  **replaces** the whole composition — do not use it to push a screen.
- **Design system:** `ui/theme/` is the only source of colour, type and shape.
  Reach for `AppTheme.colors` / `AppTheme.typography` / `AppTheme.dimensions`,
  never a raw hex or a `res/values` colour. `AppTheme(darkTheme = …)` takes the
  mode as a parameter so the in-app preference wins over the system setting.
- **Fonts:** the original Pocket brand faces (Graphik LCG, Doyle) were
  commercially licensed and are gone, replaced by open-licensed equivalents in
  `ui/src/main/assets/fonts/`: **Inter** for UI sans and **Source Serif 4** for
  display and reading text, both OFL-1.1. Compose loads them via
  `AppFontFamily`; the reader loads them by name through the `@font-face` rules
  in `app/src/main/assets/html/c/text.css`. If you change a face, change both.
- **`Backend` interface** (`backend/…/Backend.kt`): auth, `fetchEntries(cursor)`,
  `fetchArticle(id)`, add/archive/favorite/delete, tags, annotations. Each adapter
  exposes a `BackendCapabilities` for what it does not support (e.g. highlights,
  collections, server-side search). UI hides unsupported features.
- **Sync:** local-first. Reads come from Room; `SyncManager` (WorkManager) syncs
  pending mutations upstream and pulls changes downstream (delta via Readeck
  `/bookmarks/sync`, Wallabag `updatedSince`).
- **Accounts:** one active account. Tokens encrypted with Tink. Readeck uses OAuth
  device flow; Wallabag uses OAuth2 password grant.

## Conventions

- Kotlin, official Kotlin style; checked-in Android Studio code style in
  `.idea/codeStyles/Project.xml` as a base.
- Coroutines and Flow only. RxJava has been removed: the preference
  change-notification API (`utils-android/…/prefs/Store.kt`) returns `Flow`,
  backed by `SharedPreferences.OnSharedPreferenceChangeListener` in a
  `callbackFlow`. Do not reintroduce Rx.
- Version catalog in [`gradle/libs.versions.toml`](gradle/libs.versions.toml);
  Renovate keeps it updated.
- No DI framework additions; Hilt/kapt exists — use it or plain constructors.
- Keep comments rare; explain *why*, not *what*.

## Adding a new backend

1. Add a `XAdapter : Backend` in `:backend` (auth + mapping into the domain model).
2. Register it in the backend registry/factory and the server-type picker in setup.
3. Add capabilities flags for unsupported features.
4. Add MockWebServer tests for auth + one list + one mutation.
