# NeverReader

A read-it-later app for Android that talks to a server you control.

There is no account with a vendor, no telemetry, and no third party in the loop.
You point it at a [Readeck](https://readeck.org) or [Wallabag](https://wallabag.org)
instance, sign in with that server's own credentials, and your saved articles
stay on that server and on your phone.

This is a fork of [Pocket/pocket-android](https://github.com/Pocket/pocket-android).
Pocket itself was shut down in July 2025; see the
[Mozilla announcement](https://support.mozilla.org/en-US/kb/future-of-pocket).
The Android app, its sync engine and its analytics were carried forward and
rebuilt around open backends. See [Lineage](#lineage) for what changed and
[LICENSE](LICENSE) for terms.

## What it does

- Save an article from any app's share sheet, or by pasting a URL.
- Unread, favourites and archive, with sorting by date or title and local
  search.
- Read articles in an in-app reader, fetched from your server as cleaned-up HTML.
- One active account. Tokens are stored encrypted with a Tink keyset held in
  the Android keystore.

The backend interface also covers tags and highlights, and both adapters
implement them, but no screen exposes them yet. They are plumbing, not
features.

Reads come from a local Room database, so the list and the reader work offline
and never wait on the network. A WorkManager job syncs pending changes up and
pulls changes down: Readeck via its delta endpoint, Wallabag via `updatedSince`
with a periodic full refresh, because that API cannot report deletions.

## Privacy

This is a constraint the project holds to, not a feature list:

- **No tracking.** No analytics, no crash reporting, no ads, no telemetry of any
  kind. Snowplow, AppCenter, Adjust, Braze, Firebase and Sentry were all removed
  and must not come back.
- **Two permissions.** `INTERNET` and `ACCESS_NETWORK_STATE`. Nothing else.
- **No user-installed certificate authorities.** A self-hosted server needs a
  certificate from an authority the device already trusts — a private CA
  installed system-wide, or a public one. A certificate the operator generated
  themselves is rejected. Trusting the user store would let any app that can
  prompt for a CA install intercept this app's traffic, including the OAuth
  token exchange.
- **Images are same-host by default.** Article images are only loaded from the
  article's own origin unless you turn on loading from other sites.
- **The reader disables JavaScript.** It renders HTML fetched from your server
  and does not execute script from it.

Cleartext HTTP is still permitted, and that is the weakest remaining item. The
host is chosen at runtime, so the per-domain scoping that would fix it properly
lives behind an API that is not public. Denying cleartext outright would break
the plain-HTTP instances on a LAN this app exists to serve.

## Requirements

- JDK 21. Gradle 8.x will not run on newer JDKs.
- The Android SDK, via `ANDROID_HOME` or `local.properties`.
- `minSdk` 26, `targetSdk` 35.

## Build and test

```bash
export JAVA_HOME=/usr/lib/jvm/java-1.21.0-openjdk-amd64
./gradlew :app:assembleDebug :app:testDebugUnitTest :backend:test \
          :utils-android:testReleaseUnitTest :app:lintDebug
```

That is the full gate: debug APK, all unit tests, and lint. Run it without
`--offline`; the lower viewBinding artifacts the app module resolves are not in
the local cache.

Backend adapters are tested against `MockWebServer`, so the suite needs no
network and no server. The only build type that produces an installable release
is `unsignedRelease`; there is no signing config, so you will need to add your
own before distributing.

CI runs two checks on every pull request, both of which you can run locally:

```bash
./scripts/run-android-lint.sh     # :app:lintDebug
./scripts/run-license-checks.sh   # :app:licensee
```

The license check is the one that would reject an unapproved or analytics
dependency, so it is worth keeping passing.

## Modules

| Module | |
|---|---|
| [`app`](app) | The Android app: activities, navigation, screens, ViewModels. |
| [`backend`](backend) | Domain model, Room database, the `Backend` interface, the Readeck and Wallabag adapters, and sync. |
| [`ui`](ui) | Compose design system: colour, type, shape, and the shared components. |
| [`utils`](utils), [`utils-android`](utils-android) | Utilities shared across modules. |

```
app (UI) → repositories → Room DB → WorkManager sync
                              ↑
            :backend — Backend interface
                ├─ ReadeckAdapter
                └─ WallabagAdapter
```

The domain model is `Bookmark`, `Tag`, `Annotation`, `Account`. Both adapters
map their own entries into it, so UI code never sees a backend-specific type.
Each adapter declares a `BackendCapabilities` — highlights, server-side search
and delta sync — for what it cannot do. The UI does not read that yet; it is
there for a new backend to declare its limits against, not to gate features
today.

## Conventions

- Kotlin, official Kotlin style, with the checked-in
  [code style](.idea/codeStyles/Project.xml) as a base.
- Coroutines and Flow only. RxJava was removed; do not reintroduce it.
- **UI is Compose and only Compose.** There are no fragments and no XML
  layouts for screen content, and none should be added. Each activity extends
  `AbsNeverReaderActivity` and calls `setAppContent { … }`. Navigation is
  Compose Navigation: routes live in
  [`app/src/main/java/com/neverreader/app/AppNavHost.kt`](app/src/main/java/com/neverreader/app/AppNavHost.kt),
  destinations are plain `@Composable` functions named `*Screen.kt`, and each
  gets its ViewModel from `hiltViewModel()`. Add a screen as a `composable(route)`
  entry, not as an activity.
- `ui/theme/` is the only source of colour, type and shape. Reach for
  `AppTheme.colors` / `.typography` / `.dimensions`, never a raw hex or a
  `res/values` colour.
- Dependencies go in the [version catalog](gradle/libs.versions.toml), which
  Renovate keeps current.
- Comments should be rare, and explain *why* rather than *what*.

[`AGENTS.md`](AGENTS.md) has the longer version of all of this and is the file to
read before changing anything.

## Adding a backend

1. Add an `XAdapter : Backend` in `:backend`, covering auth and mapping into
   the domain model.
2. Register it in the backend registry and the server-type picker in setup.
3. Declare its `BackendCapabilities` for anything unsupported.
4. Add MockWebServer tests for auth, one list call and one mutation.

## Lineage

The original Pocket app, its GraphQL sync engine (`sync`, `sync-gen`,
`sync-parser`, `sync-pocket`) and its Snowplow analytics are gone. In their
place:

- The sync engine is replaced by direct REST calls to each backend, with
  WorkManager scheduling.
- The analytics stack is deleted outright.
- The UI is entirely Compose. The XML layouts, the fragment base classes and the
  XML navigation graph are all removed.
- Pocket's licensed brand faces are replaced with
  [Inter](https://rsms.me/inter/) for UI sans and
  [Source Serif 4](https://fonts.google.com/specimen/Source+Serif+4) for display
  and reading text, both OFL-1.1, in
  [`ui/src/main/assets/fonts`](ui/src/main/assets/fonts). If you change a face,
  change both `AppFontFamily` and the `@font-face` rules in
  `app/src/main/assets/html/c/text.css`.

Because the code came from Pocket, the history contains their commits.
[CREDITS.md](CREDITS.md) lists everyone who authored them.

## License

[MPL 2.0](LICENSE). The bundled fonts are OFL-1.1; their licenses are in
`ui/src/main/assets/fonts`.
