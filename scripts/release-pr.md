## Release {release-version}

Cut from a `release-{release-version}` branch. The template is rendered by
`scripts/open-release-pr.sh`, which is what
`.github/workflows/on-git-reference-created.yml` calls when such a branch is
pushed.

### What a release is here

There is no Play listing, no staged rollout and no beta track. A release is a
tag, and the tag is what matters:

- Pushing a `v*` tag runs [`build-apk.yml`](../.github/workflows/build-apk.yml),
  which builds a **signed** release APK when the signing secrets are present and
  attaches it to the run. Without the secrets it still builds, unsigned.
- The `unsignedRelease` build type is always unsigned and is the artifact F-Droid
  wants, since F-Droid signs its own copy.

So the practical steps are: bump the version, get the changelog together, tag,
and publish the run's artifact somewhere people can get it.

### Checklist

- [ ] `versionMajor` / `versionMinor` / `versionPatch` bumped in
      `app/build.gradle.kts` (or via `scripts/set-version-name-build-number.sh`,
      which rewrites `versionPatch`).
- [ ] Changelog drafted. The commit log between the last tag and this one is the
      source; a GitHub release with generated notes is the quickest route.
- [ ] Full gate green locally:
      ```bash
      ./gradlew :app:assembleDebug :app:testDebugUnitTest :backend:test \
                :utils-android:testReleaseUnitTest :app:lintDebug
      ```
- [ ] Merged to `main` with the pull-request checks green. The real-backend
      integration job reports rather than blocks; if it went red, look at it
      rather than ignoring it.
- [ ] Signing secrets present, or consciously shipping unsigned. The build
      ignores a partial set of credentials rather than half-applying it, so a
      missing one looks exactly like having none.
- [ ] Tag `v{release-version}` and let `build-apk.yml` run.
- [ ] Verified the uploaded APK is signed, and signed with the key you expect:
      ```bash
      apksigner verify --print-certs the-apk.apk
      ```
- [ ] Checked the manifest did not pick up a new permission. The app declares
      only `INTERNET` and `ACCESS_NETWORK_STATE`; the merged manifest also has
      WorkManager's `WAKE_LOCK`, `RECEIVE_BOOT_COMPLETED` and
      `FOREGROUND_SERVICE`. New dependencies can add their own.
- [ ] Published wherever you distribute from, with the artifact's SHA-256
      alongside it.
