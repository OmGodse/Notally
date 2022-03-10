package com.omgodse.notally.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.widget.Toast
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.chip.ChipGroup
import com.omgodse.notally.R
import com.omgodse.notally.databinding.ActivityPhoneNumberBinding
import com.omgodse.notally.miscellaneous.Operations
import com.omgodse.notally.miscellaneous.bindLabels
import com.omgodse.notally.miscellaneous.getLocale
import com.omgodse.notally.room.Type
import com.omgodse.notally.viewmodels.BaseNoteModel

class PhoneNumberActivity : NotallyActivity() {

    override val type = Type.PHONENUMBER

    override val binding by lazy { ActivityPhoneNumberBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setupListeners()
        setupToolbar(binding.Toolbar)

        if (model.isNewNote) {
            binding.EnterName.requestFocus()
        }
        setStateFromModel()
    }

    override fun getLabelGroup(): ChipGroup = binding.LabelGroup

    private fun setStateFromModel() {
        val formatter = BaseNoteModel.getDateFormatter(getLocale())

        binding.EnterName.setText(model.title)
        binding.EnterPhoneNumber.text = model.body
        binding.DateCreated.text = formatter.format(model.timestamp)

        binding.LabelGroup.bindLabels(model.labels)
    }

    private fun setupListeners() {
        binding.EnterName.doAfterTextChanged { text ->
            model.title = text.toString().trim()
        }
        binding.EnterPhoneNumber.doAfterTextChanged { text ->
            model.body = text
        }
    }
}