package com.omgodse.notally.room

import androidx.room.TypeConverter
import org.json.JSONArray
import org.json.JSONObject

object Converters {

    @TypeConverter
    fun labelsToJSON(labels: HashSet<String>) = JSONArray(labels).toString()

    @TypeConverter
    fun jsonToLabels(json: String) = JSONArray(json).iterable<String>().toHashSet()


    @TypeConverter
    fun spansToJSON(spans: List<SpanRepresentation>): String {
        val objects = spans.map { spanRepresentation -> spanRepresentation.toJSONObject() }
        return JSONArray(objects).toString()
    }

    @TypeConverter
    fun jsonToSpans(json: String): List<SpanRepresentation> {
        val iterable = JSONArray(json).iterable<JSONObject>()
        return iterable.map { jsonObject -> SpanRepresentation.fromJSONObject(jsonObject) }
    }


    @TypeConverter
    fun itemsToJson(items: List<ListItem>): String {
        val objects = items.map { listItem -> listItem.toJSONObject() }
        return JSONArray(objects).toString()
    }

    @TypeConverter
    fun jsonToItems(json: String): List<ListItem> {
        val iterable = JSONArray(json).iterable<JSONObject>()
        return iterable.map { jsonObject -> ListItem.fromJSONObject(jsonObject) }
    }

    @TypeConverter
    fun phoneItemsToJson(items: List<PhoneItem>): String {
        val objects = items.map { phoneItem -> phoneItem.toJSONObject() }
        return JSONArray(objects).toString()
    }

    @TypeConverter
    fun jsonToPhoneItems(json: String): List<PhoneItem> {
        return if (json.isEmpty()) {
            emptyList()
        } else {
            val iterable = JSONArray(json).iterable<JSONObject>()
            iterable.map { jsonObject -> PhoneItem.fromJSONObject(jsonObject) }
        }
    }


    private fun <T> JSONArray.iterable(): Iterable<T> {
        return Iterable {
            object : Iterator<T> {
                var nextIndex = 0

                override fun next(): T {
                    val element = get(nextIndex)
                    nextIndex++
                    return element as T
                }

                override fun hasNext(): Boolean {
                    return nextIndex < length()
                }
            }
        }
    }
}