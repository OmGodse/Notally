package com.omgodse.notally.activities

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.Spanned
import android.text.TextWatcher
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.util.Patterns
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.activity.viewModels
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.omgodse.notally.NotallyLinkMovementMethod
import com.omgodse.notally.R
import com.omgodse.notally.databinding.ActivityTakeNoteBinding
import com.omgodse.notally.helpers.NotesHelper
import com.omgodse.notally.miscellaneous.getLocale
import com.omgodse.notally.miscellaneous.setOnNextAction
import com.omgodse.notally.viewmodels.TakeNoteModel
import java.text.SimpleDateFormat

class TakeNote : NotallyActivity() {

    private lateinit var binding: ActivityTakeNoteBinding
    private val model: TakeNoteModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityTakeNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)

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

    override fun receiveSharedNote() {
        val title = intent.getStringExtra(Intent.EXTRA_SUBJECT)

        val plainTextBody = intent.getStringExtra(Intent.EXTRA_TEXT)
        val spannableBody = intent.getCharSequenceExtra(EXTRA_SPANNABLE) as Spannable?
        val body = spannableBody ?: plainTextBody

        body?.let {
            model.body = Editable.Factory.getInstance().newEditable(it)
        }
        title?.let {
            model.title = it
        }
    }

    override fun getViewModel() = model


    private fun setupListeners() {
        binding.EnterTitle.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
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
                model.saveNote()
            }
        })

        model.labels.observe(this, { labels ->
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
        setupMovementMethod()

        binding.EnterBody.customSelectionActionModeCallback = object : ActionMode.Callback {
            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?): Boolean {
                when (item?.itemId) {
                    R.id.Bold -> {
                        applySpan(StyleSpan(Typeface.BOLD))
                        mode?.finish()
                    }
                    R.id.Link -> {
                        applySpan(URLSpan(null))
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
                binding.EnterBody.isActionModeOn = true
                mode?.menuInflater?.inflate(R.menu.formatting, menu)
                return true
            }

            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = false

            override fun onDestroyActionMode(mode: ActionMode?) {
                binding.EnterBody.isActionModeOn = false
            }
        }
    }

    private fun setupMovementMethod() {
        val movementMethod = NotallyLinkMovementMethod {
            val builder = MaterialAlertDialogBuilder(this)
            builder.setItems(R.array.linkOptions) { dialog, which ->
                if (which == 0) {
                    val spanStart = binding.EnterBody.text?.getSpanStart(it)
                    val spanEnd = binding.EnterBody.text?.getSpanEnd(it)

                    ifBothNotNullAndInvalid(spanStart, spanEnd) { start, end ->
                        val text = binding.EnterBody.text?.substring(start, end)
                        text?.let {
                            val link = getURLFrom(it)
                            val uri = Uri.parse(link)

                            val intent = Intent(Intent.ACTION_VIEW, uri)
                            try {
                                startActivity(intent)
                            } catch (e: Exception) {
                                Toast.makeText(this, R.string.cant_open_link, Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
            builder.show()
        }
        binding.EnterBody.movementMethod = movementMethod
    }


    private fun removeSpans() {
        val selectionEnd = binding.EnterBody.selectionEnd
        val selectionStart = binding.EnterBody.selectionStart

        ifBothNotNullAndInvalid(selectionStart, selectionEnd) { start, end ->
            val styleSpans = binding.EnterBody.text?.getSpans(start, end, StyleSpan::class.java)
            styleSpans?.forEach { span ->
                binding.EnterBody.text?.removeSpan(span)
            }

            val linkSpans = binding.EnterBody.text?.getSpans(start, end, URLSpan::class.java)
            linkSpans?.forEach { span ->
                binding.EnterBody.text?.removeSpan(span)
            }

            val typefaceSpans = binding.EnterBody.text?.getSpans(start, end, TypefaceSpan::class.java)
            typefaceSpans?.forEach { span ->
                binding.EnterBody.text?.removeSpan(span)
            }

            val strikethroughSpans = binding.EnterBody.text?.getSpans(start, end, StrikethroughSpan::class.java)
            strikethroughSpans?.forEach { span ->
                binding.EnterBody.text?.removeSpan(span)
            }

            model.saveNote()
        }
    }

    private fun applySpan(spanToApply: Any) {
        val selectionEnd = binding.EnterBody.selectionEnd
        val selectionStart = binding.EnterBody.selectionStart

        ifBothNotNullAndInvalid(selectionStart, selectionEnd) { start, end ->
            binding.EnterBody.text?.setSpan(spanToApply, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            model.saveNote()
        }
    }

    private fun ifBothNotNullAndInvalid(start: Int?, end: Int?, function: (start: Int, end: Int) -> Unit) {
        if (start != null && start != -1 && end != null && end != -1) {
            function.invoke(start, end)
        }
    }


    companion object {
        const val EXTRA_SPANNABLE = "com.omgodse.notally.EXTRA_SPANNABLE"

        fun getURLFrom(text: String): String {
            return when {
                text.matches(Patterns.PHONE.toRegex()) -> "tel:$text"
                text.matches(Patterns.EMAIL_ADDRESS.toRegex()) -> "mailto:$text"
                text.matches(Patterns.DOMAIN_NAME.toRegex()) -> "http://$text"
                else -> text
            }
        }
    }
}