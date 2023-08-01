package com.omgodse.notally.activities

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.text.Spanned
import android.text.style.CharacterStyle
import android.text.style.StrikethroughSpan
import android.text.style.StyleSpan
import android.text.style.TypefaceSpan
import android.text.style.URLSpan
import android.util.Patterns
import android.view.ActionMode
import android.view.Menu
import android.view.MenuItem
import android.widget.Toast
import androidx.core.text.getSpans
import androidx.core.widget.doAfterTextChanged
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.omgodse.notally.LinkMovementMethod
import com.omgodse.notally.R
import com.omgodse.notally.miscellaneous.add
import com.omgodse.notally.miscellaneous.setOnNextAction
import com.omgodse.notally.room.Type

class TakeNote : NotallyActivity(Type.NOTE) {

    override fun configureUI() {
        binding.EnterTitle.setOnNextAction {
            binding.EnterBody.requestFocus()
        }

        setupEditor()

        if (model.isNewNote) {
            binding.EnterBody.requestFocus()
        }
    }


    override fun setupListeners() {
        super.setupListeners()
        binding.EnterBody.doAfterTextChanged { text ->
            model.body = requireNotNull(text)
        }
    }

    override fun setStateFromModel() {
        super.setStateFromModel()
        binding.EnterBody.text = model.body
    }


    private fun setupEditor() {
        setupMovementMethod()

        binding.EnterBody.customSelectionActionModeCallback = object : ActionMode.Callback {
            override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?) = false

            override fun onActionItemClicked(mode: ActionMode?, item: MenuItem?) = false

            override fun onCreateActionMode(mode: ActionMode?, menu: Menu?): Boolean {
                binding.EnterBody.isActionModeOn = true
                // Try block is there because this will crash on MiUI as Xiaomi has a broken ActionMode implementation
                try {
                    if (menu != null) {
                        menu.add(R.string.bold, 0) {
                            applySpan(StyleSpan(Typeface.BOLD))
                            mode?.finish()
                        }
                        menu.add(R.string.link, 0) {
                            applySpan(URLSpan(null))
                            mode?.finish()
                        }
                        menu.add(R.string.italic, 0) {
                            applySpan(StyleSpan(Typeface.ITALIC))
                            mode?.finish()
                        }
                        menu.add(R.string.monospace, 0) {
                            applySpan(TypefaceSpan("monospace"))
                            mode?.finish()
                        }
                        menu.add(R.string.strikethrough, 0) {
                            applySpan(StrikethroughSpan())
                            mode?.finish()
                        }
                        menu.add(R.string.clear_formatting, 0) {
                            removeSpans()
                            mode?.finish()
                        }
                    }
                } catch (exception: Exception) {
                    exception.printStackTrace()
                }
                return true
            }

            override fun onDestroyActionMode(mode: ActionMode?) {
                binding.EnterBody.isActionModeOn = false
            }
        }
    }

    private fun setupMovementMethod() {
        val items = arrayOf(getString(R.string.edit), getString(R.string.open_link))
        val movementMethod = LinkMovementMethod { span ->
            MaterialAlertDialogBuilder(this)
                .setItems(items) { dialog, which ->
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
            binding.EnterBody.text?.getSpans<CharacterStyle>(start, end)?.forEach { span ->
                binding.EnterBody.text?.removeSpan(span)
            }
        }
    }

    private fun applySpan(span: Any) {
        val selectionEnd = binding.EnterBody.selectionEnd
        val selectionStart = binding.EnterBody.selectionStart

        ifBothNotNullAndInvalid(selectionStart, selectionEnd) { start, end ->
            binding.EnterBody.text?.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    private fun ifBothNotNullAndInvalid(start: Int?, end: Int?, function: (start: Int, end: Int) -> Unit) {
        if (start != null && start != -1 && end != null && end != -1) {
            function.invoke(start, end)
        }
    }


    companion object {

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