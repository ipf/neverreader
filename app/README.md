# The app module

The Android app: the activities, the Compose screens, the ViewModels, and the
sign-in flow. See the [root README](../README.md) for the build and the privacy
constraints, and [`AGENTS.md`](../AGENTS.md) for the conventions to follow.

Everything here is `com.neverreader.*`. The `com.pocket.*` packages this module
was imported as are gone, along with the Play Store listing, the sync engine and
the analytics. What is left is a thin shell over [`:backend`](../backend): the UI
never sees a backend-specific type, it only ever sees the domain model.

## Layout

| | |
|---|---|
| `settings/Theme.kt` | The light/dark preference. `SYSTEM` is the default and resolves against the calling context. |
| `settings/SystemDarkTheme.kt` | Gone. It was the only writer of the `appTheme` preference and was never constructed, so the preference sat at its default. |
| `list/` | The saved list: `MyListViewModel`, `MyListScreen`, and the paging/filtering in `list/ListManager.kt`. |
| `reader/` | The in-app reader. The article HTML comes from the backend; the WebView is only there because it is the platform's renderer. |
| `add/` | `AddActivity`, the `ACTION_SEND` share target, and the URL-from-intent parsing behind it. |
| `auth/` | Server setup. `AuthenticationActivity` is also the OAuth redirect target. |
| `repository/` | Thin wrappers over `:backend`, injected by Hilt. |
| `sdk/util/AbsNeverReaderActivity.kt` | The activity base class: the window theme, the Compose root, and the clipboard prompt. |
| `sdk/preferences/AppPrefs.kt` | App-level preferences. There is exactly one, `LOAD_THIRD_PARTY_IMAGES`. |

## Adding a screen

Not an activity. A screen is a `@Composable` in a `*Screen.kt` file, registered
as a `composable(route)` in [`AppNavHost.kt`](src/main/java/com/neverreader/app/AppNavHost.kt),
and given its ViewModel with `hiltViewModel()`. Each activity extends
`AbsNeverReaderActivity` and hands its content over with `setAppContent { … }`.

`setAppContent` may be called again to replace the composition, so it is not a
way to push another screen.

## Signing in

`AuthenticationActivity` handles two very different things: it hosts the setup
screen, and it is the destination of Readeck's OAuth redirect. The redirect
arrives either as a cold start or through `onNewIntent` (`launchMode` is
`singleTask`), and both paths go through `handleRedirect`.

See the root README for what the flow does and `dev/README.md` for how to run a
real Readeck or Wallabag to try it against.
