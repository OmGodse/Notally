package com.omgodse.notally.room

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

@Parcelize
data class ListItem(var body: String, var checked: Boolean) : Parcelable {

    fun toJSONObject(): JSONObject {
        return JSONObject()
            .put(bodyTag, body)
            .put(checkedTag, checked)
    }

    companion object {

        private const val bodyTag = "body"
        private const val checkedTag = "checked"

        fun fromJSONObject(jsonObject: JSONObject): ListItem {
            val body = jsonObject.getString(bodyTag)
            val checked = jsonObject.getBoolean(checkedTag)
            return ListItem(body, checked)
        }
    }
}