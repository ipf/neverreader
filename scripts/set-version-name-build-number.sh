#!/usr/bin/env bash

# Fail if any commands fails.
set -e

BUILD_NUMBER=$1

# Was Pocket/build.gradle.kts and $versionBuild, neither of which exists any
# more: the module was renamed to :app, and the build number is now derived
# from versionPatch. The sed had been matching nothing, so releases were
# silently not being bumped.
SOURCE_FILE=app/build.gradle.kts
SOURCE_PATTERN='^val versionPatch = .*$'
REPLACEMENT="val versionPatch = $BUILD_NUMBER"

sed -i'.bkp' -e "s|$SOURCE_PATTERN|$REPLACEMENT|" "$SOURCE_FILE"
# sed is a "stream editor", used here to edit a file
#  -i modifies the file in place (writes the edited version to the same file)
#  '.bkp' after -i specifies an extension to use for the backup file
#         we don't need a backup file, but on macos it is required, so add it for portability
#  -e specifies the command to run
#     our command finds the patch version in the gradle script and updates it to a new value
#  last argument is the path to the file to edit

rm -f "$SOURCE_FILE.bkp"
