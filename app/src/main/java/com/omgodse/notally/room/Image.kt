package com.omgodse.notally.room

import kotlinx.parcelize.Parcelize

@Parcelize
data class Image(var name: String, val mimeType: String) : Attachment