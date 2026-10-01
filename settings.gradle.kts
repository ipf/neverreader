plugins {
    id("org.danilopianini.gradle-pre-commit-git-hooks") version "2.1.0"
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

// The hooks plugin walks up from the root looking for a .git folder and throws
// if it finds none, which kills the build outright. F-Droid checks out the
// source without a .git directory, so guard the wiring. The plugin itself is
// harmless when the hooks are never configured.
if (file(".git").isDirectory) {
    gitHooks {
        preCommit {
            from(file("scripts/pre-commit.sh"))
        }
        createHooks(overwriteExisting = true)
    }
}

rootProject.name = "neverreader"
include(":app")
include(":backend")
include(":ui")
include(":utils-android")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
