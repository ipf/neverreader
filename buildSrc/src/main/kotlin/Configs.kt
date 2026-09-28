import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

object AndroidConfigs {
    const val CompileSdkVersion = "android-37.1"
    const val MinSdkVersion = 26

    /**
     * Was never set, so AGP pinned it to [MinSdkVersion] - API 26, from 2018.
     * Android showed the "built for an older version" dialog on every launch and
     * most modern security defaults stayed switched off.
     *
     * 35, which enforces edge-to-edge. That is only safe now that the XML
     * scaffold is gone: the activity root is a single ComposeView, so the
     * window insets are applied in one place rather than on every screen.
     */
    const val TargetSdkVersion = 35
}

object KotlinConfigs {
    val jvmTarget : JvmTarget = JvmTarget.JVM_11
    const val FreeCompilerArgs = "-Xjvm-default=all"
}

object JavaConfigs {
    val javaVersion = JavaVersion.VERSION_11
}

/**
 * Set android configurations for a [CommonExtension] (the AGP 9+ extension used by
 * android app and library modules).
 */
fun CommonExtension.setDefaultConfigs() {
    compileSdkVersion(AndroidConfigs.CompileSdkVersion)
    defaultConfig.minSdk = AndroidConfigs.MinSdkVersion
    compileOptions.sourceCompatibility = JavaConfigs.javaVersion
    compileOptions.targetCompatibility = JavaConfigs.javaVersion
}

fun KotlinAndroidProjectExtension.setDefaultConfigs() {
    compilerOptions {
        jvmTarget.set(KotlinConfigs.jvmTarget)
        freeCompilerArgs.add(KotlinConfigs.FreeCompilerArgs)
    }
}

fun KotlinJvmProjectExtension.setDefaultConfigs() {
    compilerOptions {
        jvmTarget.set(KotlinConfigs.jvmTarget)
        freeCompilerArgs.add(KotlinConfigs.FreeCompilerArgs)
    }
}
