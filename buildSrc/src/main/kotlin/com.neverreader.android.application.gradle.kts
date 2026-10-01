plugins {
    id("com.android.application")
}

android {
    setDefaultConfigs()
}
androidComponents {
    beforeVariants {
        (it as com.android.build.api.variant.HasHostTestsBuilder)
            .hostTests[com.android.build.api.variant.HostTestBuilder.UNIT_TEST_TYPE]!!.enable = true
    }
}
kotlin {
    setDefaultConfigs()
}
