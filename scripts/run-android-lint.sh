#!/usr/bin/env bash

# Fail if any commands fails.
set -e

# :Pocket was renamed to :app. The Android library modules carry no sources of
# their own that need linting beyond the app.
./gradlew :app:lintDebug
