package com.omgodse.notally.preferences

import androidx.lifecycle.MutableLiveData

// LiveData that doesn't accept null values
class BetterLiveData<T>(value: T) : MutableLiveData<T>(value) {

    override fun getValue(): T {
        return requireNotNull(super.getValue())
    }
}