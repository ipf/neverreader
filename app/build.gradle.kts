import utils.*
import utils.neverreader.BuildTypes

plugins {
    id("com.android.application")
    neverReaderAndroidApp()
    kotlinKsp()
    kotlinCompose()
    hilt()
    safeArgsKotlin()
    kotlinSerialization()
    licensee()
    aboutLibraries()
}

val versionMajor = 1
val versionMinor = 0
val versionPatch = 0

android {
    namespace = "com.neverreader.app"

    defaultConfig {
        applicationId = "com.neverreader"

        buildStringField("GIT_SHA", getGitSha())

        versionCode = versionMajor * 1000000 + versionMinor * 1000 + versionPatch
        versionName = "$versionMajor.$versionMinor.$versionPatch"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        getByName(BuildTypes.DEBUG) {
            isMinifyEnabled = false
            isDebuggable = true
            matchingFallbacks.add("release")
        }

        register(BuildTypes.UNSIGNED_RELEASE) {
            isMinifyEnabled = false
            isDebuggable = false
            signingConfig = null
            matchingFallbacks.add("release")
        }
    }

    packaging {
        resources {
            merges.add("META-INF/LICENSE.txt")
            merges.add("META-INF/LICENSE")
            merges.add("META-INF/NOTICE.txt")
            merges.add("META-INF/NOTICE")
            merges.add("META-INF/ASL2.0")
            excludes.add("build-data.properties") // tink causes build issues without this
        }
    }

    lint {
        checkReleaseBuilds = false
        checkDependencies = true
        disable += "UnusedResources"
        // Records the security findings we knowingly accept (see lint.xml) so
        // they stay visible in the report instead of being ignored away.
        baseline = file("lint-baseline.xml")
    }
    buildFeatures {
        viewBinding = true
        compose = true
        buildConfig = true
    }
}

licensee {
    allow("Apache-2.0")
    allow("MIT")
    allowUrl("https://jsoup.org/license") { because("self-hosted MIT") }
    allowUrl("https://github.com/facebook/shimmer-android/blob/master/LICENSE") { because("self-hosted BSD") }
    allow("BSD-2-Clause")
    allowUrl("http://opensource.org/licenses/BSD-2-Clause")
    allowUrl("https://raw.githubusercontent.com/ThreeTen/threetenbp/master/LICENSE.txt") { because("self-hosted BSD") }
    allow("MPL-1.1")
    allow("CC0-1.0")
    allow("OFL-1.1") // Inter, substituting for the licensed Graphik brand font
    allowUrl("https://developer.android.com/studio/terms.html") { because("Android SDK") }
}

dependencies {
    implementation(projects.backend)
    implementation(projects.ui)
    implementation(projects.utilsAndroid)

    implementation(libs.androidx.activity)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.runtime)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.viewbinding)
    implementation(libs.accompanist.drawablepainter)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.adaptive)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.fragment)
    implementation(libs.androidx.fragment.compose)
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.paging)
    implementation(libs.androidx.paging.compose)

    implementation(libs.androidx.browser)
    implementation(libs.androidx.media)
    implementation(libs.androidx.work)

    implementation(Deps.AndroidX.SwipeRefreshLayout.swipeRefresh)
    implementation(Deps.AndroidX.Lifecycle.viewmodel)
    implementation(Deps.AndroidX.Lifecycle.viewmodelKtx)
    implementation(Deps.AndroidX.Lifecycle.viewmodelCompose)

    implementation(libs.kotlinx.serialization.json)

    implementation(libs.dagger.hilt)
    ksp(libs.dagger.hilt.compiler)

    implementation(libs.okhttp)
    implementation(libs.okhttp.logginginterceptor)

    implementation(libs.markwon)
    implementation(Deps.JSoup.jsoup)
    implementation(Deps.Google.JUniversalCharDet.juniversalchardet)

    implementation(Deps.JakeWharton.ThreeTenAbp.threeTen)

    implementation(libs.aboutlibraries)


    testImplementation(Deps.Mockito.core)
    testImplementation(Deps.AssertJ.core)
    testImplementation(libs.kotlin.junit)
    testImplementation(Deps.MockK.mockk)
    testImplementation(platform(libs.kotlinx.coroutines.bom))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.turbine)
    testImplementation(libs.robolectric)

    androidTestImplementation(Deps.AndroidX.Test.rules)
    androidTestImplementation(libs.kotlin.junit)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}

