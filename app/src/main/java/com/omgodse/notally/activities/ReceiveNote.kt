package com.omgodse.notally.activities

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.omgodse.notally.R
import com.omgodse.notally.databinding.ActivityReceiveNoteBinding
import com.omgodse.notally.helpers.NotesHelper
import com.omgodse.notally.miscellaneous.getFilteredSpans
import com.omgodse.notally.miscellaneous.setOnNextAction
import com.omgodse.notally.xml.Note
import java.io.File
import java.util.*
import kotlin.collections.HashSet

class ReceiveNote : AppCompatActivity() {

    private lateinit var binding: ActivityReceiveNoteBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReceiveNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setFinishOnTouchOutside(true)

        binding.EnterTitle.setOnNextAction {
            binding.EnterBody.requestFocus()
        }

        if (intent.action == Intent.ACTION_SEND) {
            handleSharedNote()
        }
    }

    private fun handleSharedNote() {
        val title = intent.getStringExtra(Intent.EXTRA_SUBJECT)
        val body = intent.getCharSequenceExtra(Intent.EXTRA_TEXT)

        binding.SaveButton.setOnClickListener {
            val enteredTitle = binding.EnterTitle.text.toString().trim()
            val enteredBody = binding.EnterBody.text

            if (enteredTitle.isNotEmpty() || enteredBody.isNotEmpty()) {
                saveNote(enteredTitle, enteredBody)
                Toast.makeText(this, getString(R.string.saved_to_notally), Toast.LENGTH_LONG).show()
            }
            finish()
        }

        binding.EnterBody.setText(body)
        binding.EnterTitle.setText(title)
    }

    private fun saveNote(title: String, body: Editable) {
        val notesHelper = NotesHelper(this)

        val timestamp = Date().time.toString()
        val timeModified: String = Date().time.toString()
        val file = File(notesHelper.getNotePath(), "$timestamp.xml")

        val note = Note(title, file.path, HashSet(), timestamp, timeModified, body.toString().trimEnd(), body.getFilteredSpans())
        note.writeToFile()

        finish()
    }
}