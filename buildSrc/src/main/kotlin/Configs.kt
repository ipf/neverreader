import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

object AndroidConfigs {
    const val CompileSdkVersion = "android-37.1"
    const val MinSdkVersion = 26
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
