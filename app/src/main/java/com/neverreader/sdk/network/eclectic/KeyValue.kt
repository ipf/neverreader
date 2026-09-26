package com.neverreader.sdk.network.eclectic

class KeyValue(val key: String?, val value: String?) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || javaClass != other.javaClass) return false

        val keyValue = other as KeyValue

        if (if (key != null) (key != keyValue.key) else keyValue.key != null) return false
        if (if (value != null) (value != keyValue.value) else keyValue.value != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = key?.hashCode() ?: 0
        result = 31 * result + (value?.hashCode() ?: 0)
        return result
    }
}
