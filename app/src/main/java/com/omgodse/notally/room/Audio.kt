package com.omgodse.notally.room

import kotlinx.parcelize.Parcelize

@Parcelize
data class Audio(var name: String, val duration: Long, val timestamp: Long) : Attachment