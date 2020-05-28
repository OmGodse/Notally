package com.omgodse.notally.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import java.io.File
import java.util.*
import kotlin.collections.HashSet

abstract class BaseModel : ViewModel() {

    var isNewNote = true
    var isFirstInstance = true

    var title = String()
    var timestamp = Date().time

    val labels = MutableLiveData(HashSet<String>())

    var file: File? = null
        set(value) {
            field = value
            setStateFromFile()
        }

    abstract fun saveNote()

    abstract fun setStateFromFile()
}