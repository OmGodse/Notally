package com.omgodse.notally.activities

import android.graphics.Typeface
import android.os.Bundle
import android.text.Editable
import android.text.InputType
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.view.*
import android.view.inputmethod.EditorInfo
import androidx.activity.viewModels
import androidx.lifecycle.Observer
import com.google.android.material.button.MaterialButton
import com.omgodse.notally.R
import com.omgodse.notally.databinding.ActivityTakeNoteBinding
import com.omgodse.notally.helpers.NotesHelper
import com.omgodse.notally.interfaces.LabelListener
import com.omgodse.notally.parents.NotallyActivity
import com.omgodse.notally.viewmodels.TakeNoteViewModel
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.HashSet

class TakeNote : NotallyActivity() {

    private lateinit var binding: ActivityTakeNoteBinding
    private val model: TakeNoteViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTakeNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTitle()
        setupEditor()
        setupListeners()
        setupToolbar(binding.Toolbar)

        if (model.isNewNote){
            binding.EnterTitle.requestFocus()
        }

        setStateFromModel()
    }


    override fun shareNote() {
        val notesHelper = NotesHelper(this)
        notesHelper.shareNote(model.title, model.body.toString())
    }

    override fun labelNote() {
        val notesHelper = NotesHelper(this)
        val labelListener = object : LabelListener {
            override fun onUpdateLabels(labels: HashSet<String>) {
                model.labels.value = labels
            }
        }
        notesHelper.labelNote(model.labels.value ?: HashSet(), labelListener)
    }

    override fun getViewModel() = model


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

    private fun setupListeners() {
        binding.EnterTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                model.title = text.toString().trim()
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.EnterBody.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(editable: Editable?) {
                model.body = binding.EnterBody.text
            }
        })

        model.labels.observe(this, Observer { labels ->
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

        val formatter = SimpleDateFormat(DateFormat, Locale.US)
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
    }

    private fun applySpan(spanToApply: Any) {
        val end = binding.EnterBody.selectionEnd
        val start = binding.EnterBody.selectionStart
        binding.EnterBody.text.setSpan(spanToApply, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
}