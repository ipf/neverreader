plugins {
    neverReaderAndroidLib()
    kotlinCompose()
}

android {
    namespace = "com.neverreader.ui"
    testOptions.unitTests.isIncludeAndroidResources = true
    buildFeatures {
        viewBinding = true
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.room.ktx)
    implementation(projects.utilsAndroid)

    api(Deps.AirBnb.Lottie.lottie)
    api(Deps.AndroidX.ConstraintLayout.constraintLayout)
    api(Deps.AndroidX.ViewPager2.viewPager2)
    api(Deps.Facebook.Shimmer.shimmer)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.paging.compose)
    implementation(libs.coil.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.ui.tooling.preview)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(Deps.AndroidX.SwipeRefreshLayout.swipeRefresh)
}
