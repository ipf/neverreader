plugins {
    neverReaderAndroidLib()
    kotlinCompose()
}

android {
    namespace = "com.neverreader.ui"
    testOptions.unitTests.isIncludeAndroidResources = true
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(projects.utilsAndroid)

    // ButtonBoxDrawable resolves drawables through AppCompatResources.
    api(libs.androidx.appcompat)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.coil.compose)

    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.ui.tooling.preview)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
