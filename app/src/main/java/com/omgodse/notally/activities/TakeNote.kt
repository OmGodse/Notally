package com.omgodse.notally.activities

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.Spannable
import android.text.Spanned
import android.text.style.*
import android.util.Patterns
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.core.widget.addTextChangedListener
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.omgodse.notally.LinkMovementMethod
import com.omgodse.notally.R
import com.omgodse.notally.databinding.ActivityTakeNoteBinding
import com.omgodse.notally.miscellaneous.bindLabels
import com.omgodse.notally.miscellaneous.getLocale
import com.omgodse.notally.miscellaneous.setOnNextAction
import com.omgodse.notally.viewmodels.BaseNoteModel
import com.omgodse.notally.viewmodels.TakeNoteModel

class TakeNote : NotallyActivity() {

    override val model: TakeNoteModel by viewModels()
    override val binding by lazy { ActivityTakeNoteBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.EnterTitle.setOnNextAction {
            binding.EnterBody.requestFocus()
        }

        setupEditor()
        setupListeners()
        setupToolbar(binding.Toolbar)

        if (model.isNewNote) {
            binding.EnterBody.requestFocus()
        }

        setStateFromModel()
    }


    override fun receiveSharedNote() {
        val title = intent.getStringExtra(Intent.EXTRA_SUBJECT)

        val plainTextBody = intent.getStringExtra(Intent.EXTRA_TEXT)
        val spannableBody = intent.getCharSequenceExtra(EXTRA_SPANNABLE) as? Spannable?
        val body = spannableBody ?: plainTextBody

        if (body != null) {
            model.body = Editable.Factory.getInstance().newEditable(body)
        }
        if (title != null) {
            model.title = title
        }

        Toast.makeText(this, R.string.saved_to_notally, Toast.LENGTH_SHORT).show()
    }


    override fun getLabelGroup() = binding.LabelGroup

    override fun shareNote() = shareNote(model.title, model.body)


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

    private fun setupListeners() {
        binding.EnterTitle.addTextChangedListener(onTextChanged = { text, start, count, after ->
            model.title = text.toString().trim()
        })

        binding.EnterBody.addTextChangedListener(afterTextChanged = { editable ->
            model.body = editable
        })
    }

    private fun setStateFromModel() {
        val formatter = BaseNoteModel.getDateFormatter(getLocale())

        binding.EnterTitle.setText(model.title)
        binding.EnterBody.text = model.body
        binding.DateCreated.text = formatter.format(model.timestamp)

        binding.LabelGroup.bindLabels(model.labels)
    }

    private fun setupMovementMethod() {
        val movementMethod = LinkMovementMethod { span ->
            MaterialAlertDialogBuilder(this)
                .setItems(R.array.linkOptions) { dialog, which ->
                    if (which == 1) {
                        val spanStart = binding.EnterBody.text?.getSpanStart(span)
                        val spanEnd = binding.EnterBody.text?.getSpanEnd(span)

                        ifBothNotNullAndInvalid(spanStart, spanEnd) { start, end ->
                            val text = binding.EnterBody.text?.substring(start, end)
                            if (text != null) {
                                val link = getURLFrom(text)
                                val uri = Uri.parse(link)

                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                try {
                                    startActivity(intent)
                                } catch (exception: Exception) {
                                    Toast.makeText(this, R.string.cant_open_link, Toast.LENGTH_LONG).show()
                                }
                            }
                        }
                    }
                }.show()
        }
        binding.EnterBody.movementMethod = movementMethod
    }


    private fun removeSpans() {
        val selectionEnd = binding.EnterBody.selectionEnd
        val selectionStart = binding.EnterBody.selectionStart

        ifBothNotNullAndInvalid(selectionStart, selectionEnd) { start, end ->
            binding.EnterBody.text?.getSpans(start, end, CharacterStyle::class.java)?.forEach { span ->
                binding.EnterBody.text?.removeSpan(span)
            }
        }
    }

    private fun applySpan(spanToApply: Any) {
        val selectionEnd = binding.EnterBody.selectionEnd
        val selectionStart = binding.EnterBody.selectionStart

        ifBothNotNullAndInvalid(selectionStart, selectionEnd) { start, end ->
            binding.EnterBody.text?.setSpan(spanToApply, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
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