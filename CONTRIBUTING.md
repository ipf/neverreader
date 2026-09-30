# Contributing

This is a fork of [Pocket/pocket-android](https://github.com/Pocket/pocket-android),
maintained separately. Pocket was shut down in July 2025, so this is not a
Mozilla or Pocket project and there is no team behind it — issues and pull
requests are handled by whoever is maintaining the fork.

## Before you start

Open an issue describing the problem or the change. It is worth checking it is
not already fixed on `main`, and worth discussing the approach before spending
an afternoon on it.

## Working on it

[AGENTS.md](AGENTS.md) is the file to read first. It has the conventions that
actually matter here, most of which exist because the code was imported from
Pocket and the old ways no longer apply — Compose only, no fragments, no XML
layouts, coroutines and Flow only, no analytics of any kind.

The short version:

```bash
export JAVA_HOME=/usr/lib/jvm/java-1.21.0-openjdk-amd64
./gradlew :app:assembleDebug :app:testDebugUnitTest :backend:test \
          :utils-android:testReleaseUnitTest :app:lintDebug
```

That is the full gate and it should be green before you open a pull request.

## If you need a real server

[`dev/README.md`](dev/README.md) runs Readeck and Wallabag in Docker with a demo
user and twenty seeded articles, and has a smoke test that talks to both. It is
much easier to test a backend change against a real one than against a mock.
