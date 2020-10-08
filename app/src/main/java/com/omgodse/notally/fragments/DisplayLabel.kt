package com.omgodse.notally.fragments

import androidx.lifecycle.MutableLiveData
import com.omgodse.notally.R
import com.omgodse.notally.helpers.OperationsHelper
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.miscellaneous.Operation
import com.omgodse.notally.xml.BaseNote

class DisplayLabel : NotallyFragment() {

    override fun getObservable(): MutableLiveData<ArrayList<BaseNote>>? {
        val label = arguments?.getString(Constants.argLabelKey)!!
        return model.getLabelledNotes(label)
    }

    override fun getFragmentID() = R.id.DisplayLabelFragment

    override fun getBackground() = R.drawable.label

    override fun getSupportedOperations(operationsHelper: OperationsHelper, baseNote: BaseNote): ArrayList<Operation> {
        return ArrayList()
    }
}