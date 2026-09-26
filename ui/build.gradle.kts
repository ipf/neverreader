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
    implementation(libs.androidx.compose.runtime.rxjava2)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.ui.tooling.preview)

    implementation(Deps.AndroidX.SwipeRefreshLayout.swipeRefresh)

    implementation(Deps.Google.FlexBox.flexbox)
    implementation(Deps.Nikartm.imageSupport)
}
