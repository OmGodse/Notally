package com.omgodse.notally.activities

import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.google.android.material.button.MaterialButton
import com.omgodse.notally.R
import com.omgodse.notally.databinding.ActivityTakeNoteBinding
import com.omgodse.notally.helpers.NotesHelper
import com.omgodse.notally.miscellaneous.getLocale
import com.omgodse.notally.miscellaneous.setOnNextAction
import com.omgodse.notally.viewmodels.TakeNoteModel
import java.text.SimpleDateFormat
import java.util.*

class TakeNote : NotallyActivity() {

    private lateinit var binding: ActivityTakeNoteBinding
    private val model: TakeNoteModel by viewModels()
    private lateinit var savedNote: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTakeNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        savedNote = model.body.toString()

        binding.EnterTitle.setOnNextAction {
            binding.EnterBody.requestFocus()
        }

        setupEditor()
        setupListeners()
        setupToolbar(binding.Toolbar)

        if (model.isNewNote) {
            binding.EnterTitle.requestFocus()
        }

        setStateFromModel()
    }


    override fun shareNote() {
        val notesHelper = NotesHelper(this)
        notesHelper.shareNote(model.title, model.body)
    }

    override fun getViewModel() = model


    private fun setupListeners() {
        binding.EnterTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                if (model.title !=text.toString()) {
                    model.timeModified = Date().time
                }
                model.title = text.toString().trim()
                model.saveNote()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.EnterBody.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(editable: Editable?) {
                model.body = binding.EnterBody.text
                if (savedNote!=model.body.toString()){
                    model.timeModified = Date().time
                    model.saveNote()
                    savedNote = model.body.toString()
                } else{
                    model.saveNote()
                }
            }
        })

        model.labels.observe(this, Observer { labels ->
            model.saveNote()
            binding.LabelGroup.removeAllViews()
            labels?.forEach { label ->
                val displayLabel = View.inflate(this, R.layout.chip_label, null) as MaterialButton
                displayLabel.text = label
                binding.LabelGroup.addView(displayLabel)
            }
        })
    }

    private fun setStateFromModel() {
        binding.EnterTitle.setText(model.title)
        binding.EnterBody.text = model.body

        val formatter = SimpleDateFormat(DateFormat, getLocale())
        binding.DateCreated.text = formatter.format(model.timestamp)
    }


    private fun setupEditor() {
        binding.EnterBody.customSelectionActionModeCallback = object : ActionMode.Callback {
            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                when (item?.itemId) {
                    R.id.Bold -> {
                        applySpan(StyleSpan(Typeface.BOLD))
                        mode?.finish()
                    }
                    R.id.Italic -> {
                        applySpan(StyleSpan(Typeface.ITALIC))
                        mode?.finish()
                    }
                    R.id.Monospace -> {
                        applySpan(TypefaceSpan("monospace"))
                        mode?.finish()
                    }
                    R.id.Strikethrough -> {
                        applySpan(StrikethroughSpan())
                        mode?.finish()
                    }
                    R.id.ClearFormatting -> {
                        removeSpans()
                        mode?.finish()
                    }
                }
                return false
            }

            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                mode?.menuInflater?.inflate(R.menu.formatting, menu)
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode?) {}
        }
    }

    private fun removeSpans() {
        val end = binding.EnterBody.selectionEnd
        val start = binding.EnterBody.selectionStart

        val styleSpans = binding.EnterBody.text.getSpans(start, end, StyleSpan::class.java)
        styleSpans.forEach { span ->
            binding.EnterBody.text.removeSpan(span)
        }

        val typefaceSpans = binding.EnterBody.text.getSpans(start, end, TypefaceSpan::class.java)
        typefaceSpans.forEach { span ->
            binding.EnterBody.text.removeSpan(span)
        }

        val strikethroughSpans = binding.EnterBody.text.getSpans(start, end, StrikethroughSpan::class.java)
        strikethroughSpans.forEach { span ->
            binding.EnterBody.text.removeSpan(span)
        }

        model.saveNote()
    }

    private fun applySpan(spanToApply: Any) {
        val end = binding.EnterBody.selectionEnd
        val start = binding.EnterBody.selectionStart
        binding.EnterBody.text.setSpan(spanToApply, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        model.saveNote()
    }
}