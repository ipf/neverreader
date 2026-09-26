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
    // Serves the @BindingAdapter annotations on the legacy Themed* views.
    // Removed in the Compose migration, together with the views themselves.
    dataBinding {
        enable = true
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
    implementation(libs.coil.network.okhttp)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.ui.tooling.preview)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test)
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    implementation(Deps.AndroidX.SwipeRefreshLayout.swipeRefresh)
}
