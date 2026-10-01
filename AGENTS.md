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

Requires **JDK 21** (what CI pins, via `.java-version`) and the Android SDK
(`ANDROID_HOME` or `local.properties`). `targetSdk` is 35, `minSdk` 26.

```bash
export JAVA_HOME=/usr/lib/jvm/java-1.21.0-openjdk-amd64
./gradlew :app:assembleDebug :app:testDebugUnitTest :backend:test :ui:test :utils-android:testReleaseUnitTest :app:lintDebug
```

Run it without `--offline`: the lower viewBinding artifacts the app module
resolves are not in the local cache.

## Building and signing

- `./gradlew :app:assembleRelease` produces an **unsigned** APK unless signing
  credentials are in the environment. There is no signing config checked in and
  there must not be: the key is generated once with
  `scripts/make-signing-secrets.sh` and lives only in GitHub secrets.
- Signing is opt-in and all-or-nothing. The build reads
  `ANDROID_SIGNING_storeFile`, `storePassword`, `keyAlias` and `keyPassword`
  (or the `android.signing.*` gradle properties), and ignores them entirely
  unless all four are set and the store file exists. A partial set is ignored
  rather than half-applied.
- `unsignedRelease` stays unsigned regardless. It is the artifact F-Droid wants,
  because F-Droid builds and signs its own copy.
- CI runs tests on pull requests and builds APKs on `main`, on tags, and on
  demand. It does not build an APK per pull request.
- Static analysis is Android Lint, plus `detekt` if you wire it in. JetBrains
  Qodana is deliberately **not** used: since 2023.2 its linters require a Qodana
  Cloud token, and the free Community edition is Java only, so it cannot check
  Kotlin or Android. Android Studio's own `inspect.sh` is no longer a usable
  entry point either, so there is no free path to Studio's full inspection set.

## Modules

- [`app/`](app) — the Android app (UI, previously `Pocket/`).
- [`backend/`](backend) — backend abstraction: domain model, Room database,
  `Backend` interface, Readeck + Wallabag adapters, sync, account management.
- [`ui/`](ui) — the Compose design system and shared components (`ui/theme/` for
  colour, type, shape and `AppTheme`; `ui/compose/` for `AppBar`, `ItemRow`,
  `FilterChips`, `SettingsList`). It also has tests: Compose UI tests run on the
  JVM under Robolectric, which needs no emulator, but it does need the
  `ComponentActivity` declared in `ui/src/test/AndroidManifest.xml` because the
  usual `ui-test-manifest` route is `debugImplementation` and this module has
  its debug variant disabled.
- [`utils-android/`](utils-android) — the preference store (`prefs/`) and the
  few Android helpers. There is no pure-JVM `utils` module: everything in it was
  dead, and what remains belongs next to the code that uses it.

## Architecture

```
app (UI) → repositories → Room DB → WorkManager sync
                              ↑
            :backend — Backend interface
                ├─ ReadeckAdapter   (Readeck's /api, OpenAPI-described)
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
- **`Backend` interface** (`backend/…/Backend.kt`): `listBookmarks(filter, limit,
  offset)`, `fetchArticleHtml(id)`, add/archive/favourite/delete, tags,
  annotations, and `changedBookmarks(since)` / `deletedBookmarkIds(since)` for
  delta sync. Auth is *not* on the interface — it is `ReadeckAuth` and
  `WallabagAuth`, and the active account is `AccountManager`.
- **`BackendCapabilities`** carries `highlights`, `serverSearch` and `deltaSync`.
  Both adapters currently set all three to true and no UI reads it, so treat it
  as a declaration a new backend can state its limits against, not a switch the
  UI consults.
- **Sync:** local-first. Reads come from Room; `SyncWorker` (WorkManager) syncs
  pending mutations upstream and pulls changes downstream (delta via Readeck
  `/bookmarks/sync`, Wallabag `updatedSince` with a periodic full refresh,
  because that API cannot report deletions).
- **Accounts:** one active account. Tokens encrypted with Tink. Readeck uses the
  OAuth authorization-code flow with PKCE, redirecting from the browser back into
  the app (device code is the fallback); Wallabag uses OAuth2 password grant.

## Conventions

- Kotlin, official Kotlin style. There is no checked-in Android Studio code
  style; `.idea/` is gitignored.
- Coroutines and Flow only. RxJava has been removed: the preference
  change-notification API (`utils-android/…/prefs/Store.kt`) returns `Flow`,
  backed by `SharedPreferences.OnSharedPreferenceChangeListener` in a
  `callbackFlow`. Do not reintroduce Rx.
- Version catalog in [`gradle/libs.versions.toml`](gradle/libs.versions.toml);
  Renovate keeps it updated.
- No DI framework additions; Hilt via KSP exists — use it or plain constructors.
- Keep comments rare; explain *why*, not *what*.

## Adding a new backend

1. Add a `XAdapter : Backend` in `:backend` (auth + mapping into the domain model).
2. Register it in the backend registry/factory and the server-type picker in setup.
3. Add capabilities flags for unsupported features.
4. Add MockWebServer tests for auth + one list + one mutation.
