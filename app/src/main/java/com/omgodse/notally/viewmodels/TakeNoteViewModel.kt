package com.omgodse.notally.viewmodels

import android.text.Editable
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

class TakeNoteViewModel : ViewModel() {

    var isFirstInstance = true
    var title = String()
    var body = Editable.Factory.getInstance().newEditable(String())
    val labels = MutableLiveData(HashSet<String>())
}