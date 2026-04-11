package com.omgodse.notally.recyclerview.viewholder

import android.text.Spannable
import android.text.SpannableString
import android.text.style.BackgroundColorSpan
import android.util.TypedValue
import android.view.MotionEvent
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.omgodse.notally.R
import com.omgodse.notally.databinding.RecyclerListItemBinding
import com.omgodse.notally.miscellaneous.setOnNextAction
import com.omgodse.notally.preferences.TextSize
import com.omgodse.notally.recyclerview.ListItemListener
import com.omgodse.notally.room.ListItem

class MakeListVH(
    val binding: RecyclerListItemBinding,
    listener: ListItemListener,
    touchHelper: ItemTouchHelper,
    textSize: String
) : RecyclerView.ViewHolder(binding.root) {

    init {
        val body = TextSize.getEditBodySize(textSize)
        binding.EditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, body)

        binding.EditText.setOnNextAction {
            listener.moveToNext(adapterPosition)
        }

        binding.EditText.doAfterTextChanged { text ->
            listener.textChanged(adapterPosition, requireNotNull(text).trim().toString())
        }

        binding.Delete.setOnClickListener {
            listener.delete(adapterPosition)
        }

        binding.CheckBox.setOnCheckedChangeListener { _, isChecked ->
            binding.EditText.isEnabled = !isChecked
            listener.checkedChanged(adapterPosition, isChecked)
        }

        binding.DragHandle.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                touchHelper.startDrag(this)
            }
            false
        }
    }

    fun bind(item: ListItem, searchKeyword: String = String()) {
        binding.root.reset()
        if (searchKeyword.isNotEmpty() && item.body.isNotEmpty()) {
            val spannable = SpannableString(item.body)
            highlightText(spannable, searchKeyword)
            binding.EditText.setText(spannable)
        } else {
            binding.EditText.setText(item.body)
        }
        binding.CheckBox.isChecked = item.checked
    }

    private fun highlightText(spannable: Spannable, keyword: String) {
        val highlightColor = itemView.context.getColor(R.color.LightBlue100)
        var index = 0
        while (index < spannable.length) {
            val matchIndex = spannable.toString().indexOf(keyword, index, ignoreCase = true)
            if (matchIndex == -1) break
            spannable.setSpan(
                BackgroundColorSpan(highlightColor),
                matchIndex,
                matchIndex + keyword.length,
                Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            index = matchIndex + keyword.length
        }
    }
}