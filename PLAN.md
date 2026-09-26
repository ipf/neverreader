# Plan: pocket-android → NeverReader (multi-backend read-it-later frontend)

NeverReader is the Pocket Android app converted into a privacy-friendly, self-hosted
read-it-later frontend. It supports multiple non-commercial backends (Readeck, Wallabag)
behind a common `Backend` interface. All tracking/analytics has been removed.

## Decisions

- **Adapt existing UI** — keep the Pocket app screens, rewire the data layer underneath.
- **Backends v1:** Readeck + Wallabag. One active account. New `Backend` interface so
  Linkding and others can be added later.
- **Auth:** Readeck = OAuth device flow; Wallabag = OAuth2 password grant.
- **Local store:** Room DB. Delta-sync via Readeck `/bookmarks/sync`, Wallabag `updatedSince`.
- **Delete:** all tracking (Snowplow/AppCenter/Adjust/Braze/Firebase/Sentry), the sync*
  modules, `analytics` module, premium/billing, slates/recommendations, feature flags.
- **Keep:** pocket-ui, utils, utils-android. Listen reworked to Android TTS.
- **Rename:** app + packages to NeverReader (applicationId `com.neverreader`).

## Target architecture

```
app (UI) → repositories → Room DB → WorkManager sync
                              ↑
            :backend — Backend interface
                ├─ ReadeckAdapter (from api.json, OpenAPI 3)
                └─ WallabagAdapter (v2/v3 REST)
pocket-ui / utils / utils-android (kept, analytics stripped)
```

## Workstreams

- **WS0** — PLAN.md + AGENTS.md.
- **WS1** — `:backend` module: domain model, Room, `Backend` interface, ReadeckAdapter,
  WallabagAdapter, SyncManager (WorkManager), AccountManager (Tink-encrypted tokens).
- **WS3** — Remove tracking & analytics: `analytics/` module, tracker call sites
  (~100 app files, ~20 pocket-ui files), AppCenter/Adjust/Braze/Firebase/Sentry init,
  deps, manifest entries, sentry.properties.
- **WS4** — Delete Pocket backend: sync, sync-android, sync-gen, sync-parser,
  sync-pocket, sync-pocket-android modules, buildSrc sync-gen tasks, app-side
  `com.pocket.sdk.api`, `sdk2/api`, `sdk/offline`, `sdk/notification`, `sdk/premium`,
  PocketSingleton; premium/slates/beta/flags features.
- **WS2** — Rewire UI: repositories over Room+Backend, `Item` → `Bookmark`, list via
  Room PagingSource, reader loads backend article HTML in `ArticleWebView`, home =
  recent saves, new server setup flow (device flow / password), Listen on Android TTS.
- **WS5** — Rename to NeverReader: packages `com.pocket` → `com.neverreader`,
  applicationId, module dirs (`Pocket` → `app`, `pocket-ui` → `ui`), build files, CI.
- **WS6** — Builds/tests/lint green (JDK 21 required for Gradle 8.x; Java 25 won't work).

## Deferred

- Linkding & other backend adapters (interface is ready for them).
- Pocket-export import via Readeck's `/bookmarks/import/pocket-file`.
- Multiple accounts/profiles.
- Old Pocket app data migration (Pocket is dead; none).

## Status: builds green

`./gradlew :app:assembleDebug :backend:test :app:lintDebug` all pass (JDK 21 required).

Core flows working: list (Room paging), reader (WebView on backend article HTML),
add via a share sheet, auth (Readeck device flow / Wallabag password), settings
(logout, licenses), background sync via WorkManager.

## Dropped in v1 (re-add when needed)

- **Listen/TTS** — the Pocket audio pipeline was deleted; rebuild on Android TTS
  reading article text from the backend.
- **Tag editor UI** (chip views) — tagging still works via the backend API.
- **Highlights/annotations UI** — the data layer supports them
  (`HighlightRepository`); the reader UI for them needs a rebuild.
- **Text settings / display settings** — reader typography prefs.
- **Old Pocket data migration** — none.
