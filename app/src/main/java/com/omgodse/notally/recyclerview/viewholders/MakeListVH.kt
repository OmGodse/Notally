package com.omgodse.notally.recyclerview.viewholders

import android.view.MotionEvent
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.omgodse.notally.databinding.RecyclerListItemBinding
import com.omgodse.notally.miscellaneous.setOnNextAction
import com.omgodse.notally.recyclerview.ListItemListener
import com.omgodse.notally.room.ListItem

class MakeListVH(val binding: RecyclerListItemBinding, listener: ListItemListener) :
    RecyclerView.ViewHolder(binding.root) {

    init {
        binding.ListItem.setOnNextAction {
            listener.onMoveToNext(adapterPosition)
        }

        binding.ListItem.doAfterTextChanged { text ->
            listener.afterTextChange(adapterPosition, text.toString().trim())
        }

        binding.CheckBox.setOnCheckedChangeListener { buttonView, isChecked ->
            binding.ListItem.isEnabled = !isChecked
            listener.checkedChanged(adapterPosition, isChecked)
        }

        binding.DragHandle.setOnTouchListener { v, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                listener.onStartDrag(this)
            }
            false
        }
    }

    fun bind(item: ListItem) {
        binding.ListItem.setText(item.body)
        binding.CheckBox.isChecked = item.checked
    }
}