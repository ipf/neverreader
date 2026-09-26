package com.neverreader.util.java

import org.apache.commons.lang3.StringUtils

/**
 * Some additional methods that aren't available in [StringUtils].
 */
object StringUtils2 {
    /**
     * @param str The string to look for (needle)
     * @param choices The strings to search (haystack)
     * @return true if str equals one of the values in choices.
     */
    fun equalsIgnoreCaseOneOf(str: CharSequence?, vararg choices: CharSequence?): Boolean {
        for (choice in choices) {
            if (StringUtils.equalsIgnoreCase(str, choice)) {
                return true
            }
        }
        return false
    }
}
