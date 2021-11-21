package com.omgodse.notally.room.livedata

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.omgodse.notally.room.BaseNote
import com.omgodse.notally.room.dao.BaseNoteDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class SearchResult(private val scope: CoroutineScope, private val baseNoteDao: BaseNoteDao) : LiveData<List<BaseNote>>() {

    private var job: Job? = null
    private var liveData: LiveData<List<BaseNote>>? = null
    private val observer = Observer<List<BaseNote>> { list -> value = list }

    init {
        value = emptyList()
    }

    fun fetch(keyword: String) {
        job?.cancel()
        liveData?.removeObserver(observer)
        job = scope.launch {
            if (keyword.isEmpty()) {
                value = emptyList()
            } else {
                liveData = baseNoteDao.getBaseNotesByKeyword(keyword)
                liveData?.observeForever(observer)
            }
        }
    }
}