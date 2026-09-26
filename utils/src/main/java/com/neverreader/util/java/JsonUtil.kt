package com.neverreader.util.java

import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonProcessingException
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.core.TreeNode
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.NumericNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.node.TextNode
import org.apache.commons.lang3.StringUtils
import org.apache.commons.lang3.math.NumberUtils
import java.io.IOException
import java.util.SortedMap
import java.util.TreeMap
import kotlin.Comparator
import kotlin.Double
import kotlin.Exception
import kotlin.Int
import kotlin.Long
import kotlin.RuntimeException
import kotlin.String
import kotlin.Throwable
import kotlin.Throws
import kotlin.math.max
import kotlin.plus

object JsonUtil {
    private var mMapper: ObjectMapper? = null
    private var mJsonFactory: JsonFactory? = null

    fun getValueAsInt(node: JsonNode, fieldName: String?, defaultValue: Int): Int {
        val value = node.get(fieldName)
        if (value == null || value.isNull()) return defaultValue

        return value.asInt(defaultValue)
    }

    fun getValueAsLong(node: JsonNode, fieldName: String?, defaultValue: Long): Long {
        val value = node.get(fieldName)
        if (value == null || value.isNull()) return defaultValue

        return value.asLong(defaultValue)
    }

    fun getValueAsText(node: JsonNode, fieldName: String?, defaultValue: String?): String? {
        val value = node.get(fieldName)
        if (value == null || value.isNull()) return defaultValue

        return getNodeAsText(value)
    }

    fun getValueAsBoolean(node: JsonNode, fieldName: String?, defaultValue: Boolean): Boolean {
        val value = node.get(fieldName)
        if (value == null || value.isNull()) return defaultValue

        return value.asBoolean(defaultValue)
    }

    fun getValueAsDouble(node: JsonNode, fieldName: String?, defaultValue: Double): Double {
        val value = node.get(fieldName)
        if (value == null || value.isNull()) return defaultValue

        return value.asDouble(defaultValue)
    }

    /**
     * Gets the value as boolean, handling the case where it is "true"/"false" or "0"/"1".
     *
     * @param node
     * @param fieldName
     * @param defaultValue
     * @return
     */
    fun getValueAsBooleanSafe(node: JsonNode?, fieldName: String?, defaultValue: Boolean): Boolean {
        if (node == null) {
            return defaultValue
        } else {
            val b = getValueAsBooleanSafe(node.get(fieldName))
            if (b == null) {
                return defaultValue
            } else {
                return b
            }
        }
    }

    fun getValueAsBooleanSafe(value: JsonNode?): Boolean? {
        if (value == null || value.isNull()) {
            return null
        } else if (value.isBoolean()) {
            return value.asBoolean()
        } else if (value.isTextual()) {
            val text = value.asText()
            if (text == "1" || text.equals("true", ignoreCase = true)) {
                return true
            } else if (text == "0" || text.equals("false", ignoreCase = true)) {
                return false
            }
        } else if (value.isNumber()) {
            if (value.asDouble() == 1.0) {
                return true
            } else if (value.asDouble() == 0.0) {
                return false
            }
        }
        return null
    }

    fun getValueAsInt(node: ArrayNode, index: Int, defaultValue: Int): Int {
        val value = node.get(index)
        if (value == null || value.isNull()) return defaultValue

        return value.asInt(defaultValue)
    }

    fun getValueAsLong(node: ArrayNode, index: Int, defaultValue: Long): Long {
        val value = node.get(index)
        if (value == null || value.isNull()) return defaultValue

        return value.asLong(defaultValue)
    }

    fun getValueAsText(node: ArrayNode, index: Int, defaultValue: String?): String? {
        val value = node.get(index)
        if (value == null || value.isNull()) return defaultValue

        return getNodeAsText(value)
    }

    private fun getNodeAsText(node: JsonNode): String? {
        if (node.isValueNode) return node.asText()
        else return node.toString()
    }

    fun getValueAsBoolean(node: ArrayNode, index: Int, defaultValue: Boolean): Boolean {
        val value = node.get(index)
        if (value == null || value.isNull) return defaultValue

        return value.asBoolean(defaultValue)
    }

    fun getValueAsDouble(node: ArrayNode, index: Int, defaultValue: Double): Double {
        val value = node.get(index)
        if (value == null || value.isNull) return defaultValue

        return value.asDouble(defaultValue)
    }

    val objectMapper: ObjectMapper
        get() {
            if (mMapper == null) {
                mMapper =
                    ObjectMapper()
                JsonUtil.configureMapper(mMapper!!)
            }

            return mMapper!!
        }

    private fun configureMapper(mapper: ObjectMapper) {
        mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }

    fun newObjectNode(): ObjectNode {
        return objectMapper.createObjectNode()
    }

    fun newArrayNode(): ArrayNode {
        return objectMapper.createArrayNode()
    }

    fun stringToObjectNode(json: String?): ObjectNode? {
        if (StringUtils.isBlank(json)) return null

        val mapper: ObjectMapper =
            objectMapper

        try {
            return mapper.readTree(json) as ObjectNode?
        } catch (e: JsonProcessingException) {
            Logs.printStackTrace(e)
        } catch (e: IOException) {
            Logs.printStackTrace(e)
        }
        return null
    }

    fun stringToArrayNode(json: String?): ArrayNode? {
        if (StringUtils.isBlank(json)) return null

        var node: ArrayNode? = null
        val mapper: ObjectMapper =
            objectMapper

        try {
            node = mapper.readTree(json) as ArrayNode?
        } catch (e: JsonProcessingException) {
            Logs.printStackTrace(e)
        } catch (e: IOException) {
            Logs.printStackTrace(e)
        }
        return node
    }

    fun getValueAsObject(array: ArrayNode, index: Int): ObjectNode? {
        return returnObjectNode(array.get(index))
    }

    fun getValueAsObject(obj: ObjectNode, key: String?): ObjectNode? {
        return returnObjectNode(obj.get(key))
    }

    private fun returnObjectNode(node: JsonNode?): ObjectNode? {
        if (node == null || !node.isObject) return null

        return node as ObjectNode
    }

    fun getValueAsArray(array: ArrayNode, index: Int): ArrayNode? {
        return returnArrayNode(array.get(index))
    }

    fun getValueAsArray(obj: ObjectNode, key: String?): ArrayNode? {
        return returnArrayNode(obj.get(key))
    }

    private fun returnArrayNode(node: JsonNode?): ArrayNode? {
        if (node == null || !node.isArray) return null

        return node as ArrayNode
    }

    /**
     * JsonParser.getText() will return a null value as "null". This ensures null values are returned as null instead of "null"
     *
     * @param jp
     * @return
     * @throws IOException
     * @throws JsonParseException
     */
    @Throws(JsonParseException::class, IOException::class)
    fun getText(jp: JsonParser): String? {
        return if (jp.currentToken != JsonToken.VALUE_NULL) jp.text else null
    }

    val jsonFactory: JsonFactory
        get() {
            if (mJsonFactory == null) mJsonFactory =
                JsonFactory()

            return mJsonFactory!!
        }

    /**
     * Returns an [ObjectNode] starting from the current location. The parser should be set to the opening { of the object.
     *
     * The parser will not be closed and its location will end up after the object end?. REVIEW end or after?
     *
     * @param jp
     * @return
     */
    fun getObject(jp: JsonParser?): ObjectNode? {
        try {
            val value: JsonNode? = objectMapper.readTree<JsonNode?>(jp)
            return if (value == null || value.isNull) {
                null
            } else {
                value as ObjectNode
            }
        } catch (e: Exception) {
            Logs.printStackTrace(e)
        }
        return null
    }

    /**
     * Returns an [ArrayNode] starting from the current location. The parser should be set to the opening [ of the array.
     *
     * The parser will not be closed and its location will end up after the array end?. REVIEW end or after?
     *
     * @param jp
     * @return
     */
    fun getArray(jp: JsonParser?): ArrayNode? {
        try {
            return objectMapper.readTree<TreeNode?>(jp) as ArrayNode?
        } catch (e: Exception) {
            Logs.printStackTrace(e)
        }
        return null
    }

    /**
     * Returns a multiline string of the json nicely formatted for display, including sorting
     * the keys by alphabetical order for easier scanning.
     * This is mostly for development use, it is not optimized for performance at all.
     * @param json
     * @return
     */
    fun prettyPrint(json: ObjectNode): String? {
        try {
            return objectMapper
                .writerWithDefaultPrettyPrinter()
                .with(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS)
                .writeValueAsString(
                    objectMapper.readValue<HashMap<*, *>?>(
                        json.toString(),
                        HashMap::class.java
                    )
                )
        } catch (e: IOException) {
            Logs.printStackTrace(e)
            return null
        }
    }

    fun copy(value: JsonNode): JsonNode? {
        return value.deepCopy<JsonNode?>()
    }

    /**
     * Creates a diff between two objects.
     * @param from The old
     * @param to The new
     * @return A set of additions, removals and changes from old to new
     */
    fun diff2(from: JsonNode?, to: JsonNode?, equalsFlag: EqualsFlag?): MutableList<String?> {
        val changes: MutableList<String?> = ArrayList<String?>()
        val path = ""
        diff2(changes, path, from, to, equalsFlag)
        return changes
    }

    fun diff2(
        changes: MutableList<String?>,
        path: String?,
        from: JsonNode?,
        to: JsonNode?,
        equalsFlag: EqualsFlag?
    ) {
        if (equals(from, to, equalsFlag)) {
            return
        } else if (to == null) {
            changes.add(path + " : " + from!!.asText() + " -> MISSING")
        } else if (from == null) {
            changes.add("$path : MISSING -> $to")
        } else if (from.nodeType != to.nodeType) {
            changes.add("$path : $from -> $to")
        } else if (from is ArrayNode) {
            val max = max(from.size(), to.size())
            for (i in 0..<max) {
                val f = if (i < from.size()) from.get(i) else null
                val t = if (i < from.size()) to.get(i) else null
                diff2(changes, "$path[$i]", f, t, equalsFlag)
            }
        } else if (from is ObjectNode) {
            var it = to.fieldNames()
            while (it.hasNext()) {
                val key = it.next()
                val subPath = "$path.$key"
                val toValue = to.get(key)
                val fromValue = from.get(key)
                if (!from.has(key)) {
                    changes.add("$subPath: MISSING -> $toValue")
                } else {
                    diff2(changes, subPath, fromValue, toValue, equalsFlag)
                }
            }
            it = from.fieldNames()
            while (it.hasNext()) {
                val key = it.next()
                val subPath = "$path.$key"
                val fromValue = from.get(key)
                if (!to.has(key)) {
                    changes.add("$subPath : $fromValue -> MISSING")
                }
            }
        } else {
            changes.add("$path : $from -> $to")
        }
    }

    fun isNull(jsonNode: JsonNode?): Boolean {
        return jsonNode == null || jsonNode.isNull
    }

    fun <T : JsonNode?> sortKeys(`in`: T?, mapper: ObjectMapper): T? {
        if (`in` is ObjectNode) {
            val out = mapper.createObjectNode()
            val sorted: SortedMap<String?, JsonNode?> = TreeMap<String?, JsonNode?>()
            val it = `in`.fieldNames()
            while (it.hasNext()) {
                val key = it.next()
                val value = `in`.get(key)
                sorted[key] = value
            }
            for (field in sorted.entries) {
                out.set(field.key, sortKeys<JsonNode?>(field.value, mapper))
            }
            return out as T?
        } else if (`in` is ArrayNode) {
            val out = mapper.createArrayNode()
            for (i in 0..<`in`.size()) {
                out.add(sortKeys<JsonNode?>(`in`.get(i), mapper))
            }
            return out as T?
        } else {
            return `in`
        }
    }

    fun stringAllValues(`in`: JsonNode, mapper: ObjectMapper): JsonNode? {
        if (`in` is ObjectNode) {
            val out = mapper.createObjectNode()
            val it = `in`.fieldNames()
            while (it.hasNext()) {
                val key = it.next()
                out.put(key, stringAllValues(`in`.get(key), mapper))
            }
            return out
        } else if (`in` is ArrayNode) {
            val out = mapper.createArrayNode()
            for (i in 0..<`in`.size()) {
                out.add(stringAllValues(`in`.get(i), mapper))
            }
            return out
        } else if (`in`.isTextual) {
            return `in`
        } else {
            return TextNode(`in`.asText())
        }
    }

    /**
     * Produces a new instance or null, where it has all of the fields of `into` and `from`, preferring `from`'s values for
     * and fields they both have. Returns null only if both are null. Safe to pass null for either one.
     */
    fun merge(into: ObjectNode?, from: ObjectNode?): ObjectNode? {
        if (into == null && from == null) {
            return null
        }
        val obj = newObjectNode()
        if (into != null) {
            obj.setAll(into)
        }
        if (from != null) {
            obj.setAll(from)
        }
        return obj
    }

    fun equals(o1: JsonNode?, o2: JsonNode?, flag: EqualsFlag?): Boolean {
        if (o1 == null) {
            return o2 == null
        } else if (o2 == null) {
            return false
        }

        return if (flag == null) {
            o1 == o2
        } else if (flag == EqualsFlag.ANY_NUMERICAL) {
            o1.equals(NUMERIC_COMPARE, o2)
        } else if (flag == EqualsFlag.ANY_TYPE) {
            o1.equals(ANY_COMPARE, o2)
        } else {
            throw RuntimeException("unknown flag $flag")
        }
    }

    private val NUMERIC_COMPARE = Comparator { o1: JsonNode?, o2: JsonNode? ->
        if (o1 == o2) {
            return@Comparator 0
        }
        val isNumeric1 = o1 is NumericNode || NumberUtils.isParsable(o1!!.asText())
        val isNumeric2 = o2 is NumericNode || NumberUtils.isParsable(o2!!.asText())
        if (isNumeric1 && isNumeric2) {
            val d1 = o1.asDouble()
            val d2 = o2.asDouble()
            return@Comparator if (d1 == d2) 0 else 1
        } else {
            return@Comparator 1
        }
    }

    private val ANY_COMPARE = Comparator { o1: JsonNode?, o2: JsonNode? ->
        if (o1 == o2) {
            return@Comparator 0
        }
        val t1 = o1!!.asText()
        val t2 = o2!!.asText()
        val isNumeric1 = o1 is NumericNode || NumberUtils.isParsable(t1)
        val isNumeric2 = o2 is NumericNode || NumberUtils.isParsable(t2)
        if (isNumeric1 && isNumeric2) {
            val d1 = o1.asDouble()
            val d2 = o2.asDouble()
            return@Comparator if (d1 == d2) 0 else 1
        } else {
            val b1 = getValueAsBooleanSafe(o1)
            val b2 = getValueAsBooleanSafe(o2)
            if (b1 != null || b2 != null) {
                if (b1 != null && b2 != null) {
                    return@Comparator (if (b1 == b2) 0 else if (b1) 1 else -1)
                } else {
                    return@Comparator 0
                }
            }
            val str1 = if (o1.isNull) null else t1
            val str2 = if (o1.isNull) null else t2
            return@Comparator if (StringUtils.equals(str1, str2)) 0 else 1
        }
    }

    /**
     * Returns some information about current location that can be helpful for error logging.
     */
    fun errorLocation(parser: JsonParser): String {
        return try {
            " TOKEN: " + parser.currentToken() +
                " VALUE: " + parser.valueAsString +
                " NAME: " + parser.currentName +
                " LOCATION: " + parser.currentLocation
        } catch (t: Throwable) {
            "?"
        }
    }

    enum class EqualsFlag {
        /** Numerical nodes like int and double are compared in value, so 1 equals 1.0  */
        ANY_NUMERICAL,

        /**
         * Same as Numerical, but also checks for numbers as strings, so "1" equals 1.
         * Also allows 0 to equal false and 1 to equal true (including "0" and "1")
         */
        ANY_TYPE
    }
}
