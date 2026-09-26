package utils

import org.codehaus.groovy.runtime.ProcessGroovyMethods
import org.gradle.api.Project

fun getGitSha(): String =
    ProcessGroovyMethods.getText(
        ProcessGroovyMethods.execute("git rev-parse --short HEAD")
    ).trim()
