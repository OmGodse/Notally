package com.omgodse.notally.fragments

import android.animation.LayoutTransition
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import com.omgodse.notally.R
import com.omgodse.notally.activities.MainActivity
import com.omgodse.notally.helpers.NotesHelper
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.miscellaneous.Operation
import com.omgodse.notally.parents.NotallyFragment
import com.omgodse.notally.viewmodels.NoteModel
import java.io.File

class Search : NotallyFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        model.keyword = String()
        binding.FrameLayout.layoutTransition = LayoutTransition()

        (mContext as MainActivity).binding.EnterSearchKeyword.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(query: Editable?) {
                model.keyword = query.toString().trim()
                model.fetchRelevantNotes(getPayload())
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
       if (resultCode == Constants.ResultCodeEditedFile){
            val filePath = data?.getStringExtra(Constants.FilePath)
            filePath?.let {
                val file = File(filePath)
                val note = NotesHelper.convertFileToNote(file)

                if (!model.isNoteMatch(note)){
                    model.fetchRelevantNotes(getPayload())
                }
                else super.onActivityResult(requestCode, resultCode, data)
            }
        }
        else super.onActivityResult(requestCode, resultCode, data)
    }


    override fun getPayload() = NoteModel.SEARCH_RESULTS

    override fun getObservable() = model.observableSearchResults


    override fun getFragmentID() = R.id.SearchFragment

    override fun getBackground() = mContext.getDrawable(R.drawable.layout_background_search)

    override fun getSupportedOperations() : ArrayList<Operation> {
        val supportedOperations = ArrayList<Operation>()
        supportedOperations.add(Operation(R.string.share, R.drawable.share))
        supportedOperations.add(Operation(R.string.labels, R.drawable.label))
        supportedOperations.add(Operation(R.string.export, R.drawable.export))
        supportedOperations.add(Operation(R.string.delete, R.drawable.delete))
        supportedOperations.add(Operation(R.string.archive, R.drawable.archive))
        return supportedOperations
    }
}