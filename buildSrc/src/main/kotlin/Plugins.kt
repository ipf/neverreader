/**
 * Define plugins here for easy use in other modules
 */

import org.gradle.kotlin.dsl.kotlin
import org.gradle.plugin.use.PluginDependenciesSpec
import org.gradle.plugin.use.PluginDependencySpec

fun PluginDependenciesSpec.neverReaderAndroidLib(): PluginDependencySpec =
    id("com.neverreader.android.library")

fun PluginDependenciesSpec.neverReaderAndroidApp(): PluginDependencySpec =
    id("com.neverreader.android.application")

fun PluginDependenciesSpec.kotlinKsp(): PluginDependencySpec =
    id("com.google.devtools.ksp")

fun PluginDependenciesSpec.versions(): PluginDependencySpec =
    id("com.neverreader.versions")

fun PluginDependenciesSpec.hilt(): PluginDependencySpec =
    id("dagger.hilt.android.plugin")

fun PluginDependenciesSpec.safeArgsKotlin(): PluginDependencySpec =
    id("androidx.navigation.safeargs.kotlin")

fun PluginDependenciesSpec.kotlinSerialization(): PluginDependencySpec =
    kotlin("plugin.serialization")

fun PluginDependenciesSpec.kotlinCompose(): PluginDependencySpec =
    id("org.jetbrains.kotlin.plugin.compose")

fun PluginDependenciesSpec.licensee(): PluginDependencySpec =
    id("app.cash.licensee")

fun PluginDependenciesSpec.aboutLibraries(): PluginDependencySpec =
    id("com.mikepenz.aboutlibraries.plugin")
