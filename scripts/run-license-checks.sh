#!/usr/bin/env bash

# Fail if any commands fails.
set -e

# :Pocket and :sync-gen no longer exist - the modules were renamed to :app,
# :backend, :ui, :utils and :utils-android. This task was silently erroring in
# CI, so the licensee allow-list - the one check that would reject an unapproved
# or analytics dependency - was never actually running.
./gradlew :app:licensee
