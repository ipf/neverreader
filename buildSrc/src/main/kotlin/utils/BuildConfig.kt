package utils

import com.android.build.api.dsl.BaseFlavor

fun BaseFlavor.buildStringField(name: String, value: String) {
    buildConfigField("String", name, "\"$value\"")
}