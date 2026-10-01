package utils

import org.gradle.api.Project

/**
 * The short SHA of the current checkout, or a placeholder when there is no Git
 * repository to ask.
 *
 * F-Droid builds from an exported source tree with no .git, and failing the
 * build over a build-info string would be a poor trade. The SHA only ever ends
 * up in AboutLibraries metadata, so a placeholder is harmless there.
 */
fun getGitSha(project: Project): String = runCatching {
    val process = ProcessBuilder("git", "rev-parse", "--short", "HEAD")
        .directory(project.rootDir)
        .redirectErrorStream(true)
        .start()

    val output = process.inputStream.bufferedReader().readText().trim()
    if (process.waitFor() == 0) {
        output.ifEmpty { UNKNOWN_SHA }
    } else {
        UNKNOWN_SHA
    }
}.getOrElse { UNKNOWN_SHA }

private const val UNKNOWN_SHA = "unknown"