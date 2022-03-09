package com.omgodse.notally.room

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import org.json.JSONObject

@Parcelize
data class PhoneItem(var contactName: String = "", var contactNo: String = "") : Parcelable {

    fun toJSONObject(): JSONObject {
        return JSONObject()
            .put(contactNameTag, contactName)
            .put(contactNoTag, contactNo)
    }

    companion object {

        private const val contactNameTag = "name"
        private const val contactNoTag = "number"

        fun fromJSONObject(jsonObject: JSONObject): PhoneItem {
            val contactName = jsonObject.getString(contactNameTag)
            val contactNo = jsonObject.getString(contactNoTag)
            return PhoneItem(contactName, contactNo)
        }
    }
}