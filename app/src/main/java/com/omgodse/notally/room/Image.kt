package com.omgodse.notally.room

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Image(val name: String, val mimeType: String) : Parcelable