package com.neverreader.util.java

interface StringValidator {
    /** Return null if valid. If invalid, return an error message to display to user  */
    fun validate(value: String?): String?
}
