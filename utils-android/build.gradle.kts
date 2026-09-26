plugins {
    neverReaderAndroidLib()
}
android {
    namespace = "com.neverreader.utils.android"
}
dependencies {
    api(projects.utils)
    api(platform(libs.kotlinx.coroutines.bom))
    api(libs.kotlinx.coroutines.android)
    api(Deps.Google.Material.material)
    api(libs.androidx.core)
}
