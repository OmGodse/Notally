package com.omgodse.notally.room.json

import org.json.JSONException
import org.json.JSONObject

class SafeJSONObject(json: String) : JSONObject(json) {

    override fun getBoolean(name: String): Boolean {
        return try {
            super.getBoolean(name)
        } catch (exception: JSONException) {
            false
        }
    }
}