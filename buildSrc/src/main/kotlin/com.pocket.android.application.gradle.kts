plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    setDefaultConfigs()
}
androidComponents {
    beforeVariants {
        it.enableUnitTest = true
    }
}
kotlin {
    setDefaultConfigs()
}
