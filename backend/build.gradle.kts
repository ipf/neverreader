plugins {
    neverReaderAndroidLib()
    kotlinKsp()
    kotlinSerialization()
}

android {
    namespace = "com.neverreader.backend"

    // Room's generated DAO implementations need the real Android runtime, so the
    // database tests run under Robolectric with merged resources on the classpath.
    testOptions.unitTests.isIncludeAndroidResources = true
}

dependencies {
    api(platform(libs.kotlinx.coroutines.bom))
    api(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    api(libs.kotlinx.serialization.json)
    api(libs.okhttp)
    api(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.androidx.room.paging)
    ksp(libs.androidx.room.compiler)
    api(libs.androidx.paging)
    api(libs.androidx.work.runtime.ktx)
    implementation(libs.tink)
    implementation(libs.dagger.hilt)

    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.room.testing)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}
