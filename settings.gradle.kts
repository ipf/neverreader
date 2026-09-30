plugins {
    id("org.danilopianini.gradle-pre-commit-git-hooks") version "2.1.0"
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

gitHooks {
    preCommit {
        from(file("scripts/pre-commit.sh"))
    }
    createHooks(overwriteExisting = true)
}

rootProject.name = "neverreader"
include(":app")
include(":backend")
include(":ui")
include(":utils-android")

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")
