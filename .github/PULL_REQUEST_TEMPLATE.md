<!--
A sentence or two on what this changes and why. More if the reasoning is not
obvious from the diff.
-->

## References

<!-- Issue, design doc, or the server's API docs, if there is one. -->

## PR Checklist

**Before opening**
* [ ] `./gradlew :app:assembleDebug :app:testDebugUnitTest :backend:test :utils-android:testReleaseUnitTest :app:lintDebug`
      passes
* [ ] Read the diff back as a reviewer

**Things worth checking, if they apply**
* [ ] No new dependency, or the one added is necessary and its license is
      allowed (`scripts/run-license-checks.sh`)
* [ ] No new permission in `app/src/main/AndroidManifest.xml`. The app declares
      only `INTERNET` and `ACCESS_NETWORK_STATE`; anything else is a deliberate
      decision. (The merged manifest also picks up `WAKE_LOCK`,
      `RECEIVE_BOOT_COMPLETED` and `FOREGROUND_SERVICE` from WorkManager, so
      check what *you* added, not the merged list.)
* [ ] No analytics, crash reporting, ads or telemetry of any kind. Snowplow,
      AppCenter, Adjust, Braze, Firebase and Sentry must not come back
* [ ] New or changed UI is Compose in a `*Screen.kt` registered in
      `AppNavHost.kt` — no fragments, no XML layouts
* [ ] Colour, type and spacing come from `AppTheme`, not literals
* [ ] Behaviour that was previously unreachable now has a caller, rather than
      just being reachable
* [ ] Docs updated if a command, module or flow in them changed
