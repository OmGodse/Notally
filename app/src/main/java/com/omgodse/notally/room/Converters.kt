package com.omgodse.notally.room

import androidx.room.TypeConverter
import com.omgodse.notally.room.json.IterableJSONArray
import org.json.JSONArray
import org.json.JSONObject

class Converters {

    @TypeConverter
    fun labelsToJSON(labels: HashSet<String>) = JSONArray(labels).toString()

    @TypeConverter
    fun jsonToLabels(json: String) = IterableJSONArray<String>(json).toHashSet()


    @TypeConverter
    fun spansToJSON(spans: List<SpanRepresentation>): String {
        val objects = spans.map { spanRepresentation -> spanRepresentation.toJSONObject() }
        return JSONArray(objects).toString()
    }

    @TypeConverter
    fun jsonToSpans(json: String): List<SpanRepresentation> {
        val jsonArray = IterableJSONArray<JSONObject>(json)
        return jsonArray.map { jsonObject -> SpanRepresentation.fromJSONObject(jsonObject) }
    }


    @TypeConverter
    fun itemsToJson(items: List<ListItem>): String {
        val objects = items.map { listItem -> listItem.toJSONObject() }
        return JSONArray(objects).toString()
    }

    @TypeConverter
    fun jsonToItems(json: String): List<ListItem> {
        val jsonArray = IterableJSONArray<JSONObject>(json)
        return jsonArray.map { jsonObject -> ListItem.fromJSONObject(jsonObject) }
    }
}