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
     * 34 rather than 35: 35 enforces edge-to-edge, and the activity is still
     * scaffolded by a legacy view tree (activity_root.xml -> ril_root.xml) with
     * the Compose screens inside it, so opting in would mean handling window
     * insets across every screen. 34 still requires explicit android:exported,
     * PendingIntent mutability flags, and a foregroundServiceType - none of
     * which this app uses.
     */
    const val TargetSdkVersion = 34
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
