package com.omgodse.notally.viewmodels

import android.database.sqlite.SQLiteConstraintException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

fun ViewModel.executeAsyncWithCallback(function: suspend () -> Unit, callback: (success: Boolean) -> Unit) {
    viewModelScope.launch {
        val success = try {
            withContext(Dispatchers.IO) { function() }
            true
        } catch (exception: SQLiteConstraintException) {
            false
        }
        callback(success)
    }
}