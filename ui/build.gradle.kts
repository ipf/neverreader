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

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.coil.compose)

    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.ui.tooling.preview)
    // Compose UI tests run on the JVM under Robolectric: setContent needs a real
    // window, a measure pass and a recomposition, none of which a plain JVM test
    // can provide. Robolectric plus the Android resources is what makes them work.
    testImplementation(libs.kotlin.junit)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.kotlinx.coroutines.bom))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
