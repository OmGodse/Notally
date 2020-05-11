package com.omgodse.notally.activities

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.omgodse.notally.R
import com.omgodse.notally.databinding.ActivityReceiveNoteBinding
import com.omgodse.notally.helpers.NotesHelper
import com.omgodse.notally.xml.XMLTags
import com.omgodse.notally.xml.XMLWriter
import java.io.File
import java.io.FileWriter
import java.util.*

class ReceiveNote : AppCompatActivity() {

    private lateinit var binding: ActivityReceiveNoteBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReceiveNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setFinishOnTouchOutside(true)

        setupTitle()

        if (intent.action == Intent.ACTION_SEND) {
            handleSharedNote()
        }
    }

    private fun setupTitle() {
        binding.EnterTitle.setRawInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)

        binding.EnterTitle.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                binding.EnterBody.requestFocus()
                return@setOnKeyListener true
            } else return@setOnKeyListener false
        }

        binding.EnterTitle.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                binding.EnterBody.requestFocus()
                return@setOnEditorActionListener true
            } else return@setOnEditorActionListener false
        }
    }

    private fun handleSharedNote() {
        val body = intent.getStringExtra(Intent.EXTRA_TEXT)
        val title = intent.getStringExtra(Intent.EXTRA_SUBJECT)

        binding.SaveButton.setOnClickListener {
            val enteredTitle = binding.EnterTitle.text.toString().trim()
            val enteredBody = binding.EnterBody.text.toString().trim()

            if (enteredTitle.isNotEmpty() || enteredBody.isNotEmpty()) {
                saveNote(enteredTitle, enteredBody)
                Toast.makeText(this, getString(R.string.saved_to_notally), Toast.LENGTH_LONG).show()
            }
            finish()
        }

        binding.EnterBody.setText(body)
        binding.EnterTitle.setText(title)
    }

    private fun saveNote(title: String, body: String) {
        val notesHelper = NotesHelper(this)
        val filePath = notesHelper.getNotePath()

        val timestamp = Date().time.toString()

        val file = File(filePath, "$timestamp.xml")

        val fileWriter = FileWriter(file)
        val xmlWriter = XMLWriter(XMLTags.Note)

        xmlWriter.startNote()
        xmlWriter.setDateCreated(timestamp)
        xmlWriter.setTitle(title)
        xmlWriter.setBody(body)
        xmlWriter.endNote()

        fileWriter.write(xmlWriter.getText())
        fileWriter.close()
        finish()
    }
}