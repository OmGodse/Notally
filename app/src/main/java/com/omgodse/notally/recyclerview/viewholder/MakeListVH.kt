package com.omgodse.notally.recyclerview.viewholder

import android.util.TypedValue
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.inputmethod.EditorInfo
import android.widget.TextView.INVISIBLE
import android.widget.TextView.OnEditorActionListener
import android.widget.TextView.VISIBLE
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.omgodse.notally.databinding.RecyclerListItemBinding
import com.omgodse.notally.miscellaneous.setOnNextAction
import com.omgodse.notally.preferences.TextSize
import com.omgodse.notally.recyclerview.ListItemListener
import com.omgodse.notally.room.ListItem
import com.zerobranch.layout.SwipeLayout.SwipeActionsListener


class MakeListVH(
    val binding: RecyclerListItemBinding,
    val listener: ListItemListener,
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

        binding.EditText.setOnEditorActionListener(OnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_NEXT) {
                listener.add(adapterPosition + 1)
                return@OnEditorActionListener true
            }
            false
        })

        binding.EditText.setOnKeyListener { v, keyCode, event ->
            if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_DEL) {
                listener.delete(adapterPosition, false)
            }
            true
        }

        binding.EditText.setOnFocusChangeListener { _, hasFocus ->
            binding.Delete.visibility = if(hasFocus) VISIBLE else INVISIBLE
        }

        binding.Delete.setOnClickListener {
            listener.delete(adapterPosition, true)
        }

        binding.CheckBox.setOnClickListener { _ ->
            listener.checkedChanged(adapterPosition, binding.CheckBox.isChecked)
        }

        binding.DragHandle.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                touchHelper.startDrag(this)
            }
            false
        }

        binding.SwipeLayout.setOnActionsListener(object : SwipeActionsListener {
            override fun onOpen(direction: Int, isContinuous: Boolean) {
                listener.isChildItemChanged(adapterPosition, true)
            }

            override fun onClose() {
                listener.isChildItemChanged(adapterPosition, false)
            }

        })
    }

    fun bind(item: ListItem, firstItem: Boolean) {
        binding.EditText.setText(item.body)
        binding.EditText.isEnabled = !item.checked
        binding.CheckBox.isChecked = item.checked
        binding.SwipeLayout.isEnabledSwipe = !firstItem
        if (item.isChildItem) {
            if (!binding.SwipeLayout.isLeftOpen)
                binding.SwipeLayout.post { binding.SwipeLayout.openLeft(false) }
        } else {
            if (!binding.SwipeLayout.isClosed)
                binding.SwipeLayout.post { binding.SwipeLayout.close(false) }
        }
    }
}