package com.omgodse.notally.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.omgodse.notally.miscellaneous.ListItem

class MakeListViewModel : ViewModel() {

    var isFirstInstance = true
    var title = String()
    val items = ArrayList<ListItem>()
    val labels = MutableLiveData(HashSet<String>())
}