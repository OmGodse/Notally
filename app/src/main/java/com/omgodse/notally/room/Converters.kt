package com.omgodse.notally.room

import androidx.room.TypeConverter
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

object Converters {

    @TypeConverter
    fun labelsToJson(labels: List<String>) = JSONArray(labels).toString()

    @TypeConverter
    fun jsonToLabels(json: String) = JSONArray(json).iterable<String>().toList()


    @TypeConverter
    fun imagesToJson(images: List<Image>): String {
        val objects = images.map { image ->
            val jsonObject = JSONObject()
            jsonObject.put("name", image.name)
            jsonObject.put("color", image.color)
            jsonObject.put("mimeType", image.mimeType)
        }
        return JSONArray(objects).toString()
    }

    @TypeConverter
    fun jsonToImages(json: String): List<Image> {
        val iterable = JSONArray(json).iterable<JSONObject>()
        return iterable.map { jsonObject ->
            val name = jsonObject.getString("name")
            val color = jsonObject.getInt("color")
            val mimeType = jsonObject.getString("mimeType")
            Image(name, color, mimeType)
        }
    }


    @TypeConverter
    fun spansToJson(spans: List<SpanRepresentation>): String {
        val objects = spans.map { spanRepresentation ->
            val jsonObject = JSONObject()
            jsonObject.put("bold", spanRepresentation.bold)
            jsonObject.put("link", spanRepresentation.link)
            jsonObject.put("italic", spanRepresentation.italic)
            jsonObject.put("monospace", spanRepresentation.monospace)
            jsonObject.put("strikethrough", spanRepresentation.strikethrough)
            jsonObject.put("start", spanRepresentation.start)
            jsonObject.put("end", spanRepresentation.end)
        }
        return JSONArray(objects).toString()
    }

    @TypeConverter
    fun jsonToSpans(json: String): List<SpanRepresentation> {
        val iterable = JSONArray(json).iterable<JSONObject>()
        return iterable.map { jsonObject ->
            val bold = jsonObject.getSafeBoolean("bold")
            val link = jsonObject.getSafeBoolean("link")
            val italic = jsonObject.getSafeBoolean("italic")
            val monospace = jsonObject.getSafeBoolean("monospace")
            val strikethrough = jsonObject.getSafeBoolean("strikethrough")
            val start = jsonObject.getInt("start")
            val end = jsonObject.getInt("end")
            SpanRepresentation(bold, link, italic, monospace, strikethrough, start, end)
        }
    }


    @TypeConverter
    fun itemsToJson(items: List<ListItem>): String {
        val objects = items.map { item ->
            val jsonObject = JSONObject()
            jsonObject.put("body", item.body)
            jsonObject.put("checked", item.checked)
        }
        return JSONArray(objects).toString()
    }

    @TypeConverter
    fun jsonToItems(json: String): List<ListItem> {
        val iterable = JSONArray(json).iterable<JSONObject>()
        return iterable.map { jsonObject ->
            val body = jsonObject.getString("body")
            val checked = jsonObject.getBoolean("checked")
            ListItem(body, checked)
        }
    }


    private fun JSONObject.getSafeBoolean(name: String): Boolean {
        return try {
            getBoolean(name)
        } catch (exception: JSONException) {
            false
        }
    }


    private fun <T> JSONArray.iterable() = Iterable {
        object : Iterator<T> {
            var index = 0

            override fun next(): T {
                val element = get(index)
                index++
                return element as T
            }

            override fun hasNext(): Boolean {
                return index < length()
            }
        }
    }
}