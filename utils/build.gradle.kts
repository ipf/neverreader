plugins {
    kotlinJvm()
}

dependencies {
    api(platform(libs.kotlinx.coroutines.bom))
    api(libs.kotlinx.coroutines.core)
}
