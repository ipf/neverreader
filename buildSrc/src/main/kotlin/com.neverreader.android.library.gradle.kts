plugins {
    id("com.android.library")
}

android {
    setDefaultConfigs()
}
androidComponents {
    beforeVariants(selector().withBuildType("debug")) {
        it.enable = false
    }
}
kotlin {
    setDefaultConfigs()
}
