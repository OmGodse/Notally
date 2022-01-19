package com.omgodse.notally.fragments

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.View
import androidx.navigation.fragment.NavHostFragment
import com.omgodse.notally.MenuDialog
import com.omgodse.notally.R
import com.omgodse.notally.activities.MainActivity
import com.omgodse.notally.activities.MakeList
import com.omgodse.notally.activities.TakeNote
import com.omgodse.notally.miscellaneous.add

class Notes : NotallyFragment() {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        (requireContext() as MainActivity).binding.TakeNoteFAB.setOnClickListener {
            displayNoteTypes()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        menu.add(R.string.search, R.drawable.search) {
            NavHostFragment.findNavController(this).navigate(R.id.NotesToSearch)
        }
    }


    private fun displayNoteTypes() {
        MenuDialog(requireContext())
            .add(R.string.make_list, R.drawable.checkbox) { goToActivity(MakeList::class.java) }
            .add(R.string.take_note, R.drawable.edit) { goToActivity(TakeNote::class.java) }
            .show()
    }


    override fun getObservable() = model.baseNotes

    override fun getBackground() = R.drawable.notes
}