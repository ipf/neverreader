package com.neverreader.app

import org.apache.commons.lang3.StringUtils

/**
 * Help convert between versionCode and versionName based on how we format them for Pocket builds. See the Pocket build.gradle file for more details or the RELEASE_WORKFLOW.md version section.
 */
object VersionUtil {
    @JvmOverloads
    fun toVersionCode(
        versionMajor: Int,
        versionMinor: Int,
        versionPatch: Int = 0,
        versionBuild: Int = 0
    ): Int {
        return versionMajor * 10000000 + versionMinor * 100000 + versionPatch * 1000 + versionBuild
    }

    fun toVersionName(versionCode: Int): String {
        // MMM.mm.pp.bbb   <-- version names look like this  (M major, m minor, p patch, b build)
        // MMMmmppbbb      <-- version codes are made up of the various parts with leading zeros as needed
        // 0123456789      <-- string indexes of each character/part

        val str = StringUtils.leftPad(versionCode.toString(), 10, '0')
        val major = extractNumber(str, 0, 3)
        val minor = extractNumber(str, 3, 2)
        val patch = extractNumber(str, 5, 2)
        val build = extractNumber(str, 7, 3)
        return major.toString() + "." + minor + "." + patch + "." + build
    }

    private fun extractNumber(numberStr: String, start: Int, length: Int): Int {
        return numberStr.substring(start, start + length)
            .replaceFirst("^0+(?!$)".toRegex(), "").toInt() // Remove leading zeros
    }
}
