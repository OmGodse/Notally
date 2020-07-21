package com.omgodse.notally.viewholders

import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.MotionEvent
import androidx.recyclerview.widget.RecyclerView
import com.omgodse.notally.databinding.ListItemBinding
import com.omgodse.notally.interfaces.ListItemListener
import com.omgodse.notally.miscellaneous.setOnNextAction
import com.omgodse.notally.xml.ListItem

class MakeListViewHolder(private val binding: ListItemBinding, listItemListener: ListItemListener?) :
    RecyclerView.ViewHolder(binding.root) {

    init {
        binding.ListItem.setOnNextAction {
            listItemListener?.onMoveToNext(adapterPosition)
        }

        binding.CheckBox.setOnCheckedChangeListener { buttonView, isChecked ->
            binding.ListItem.paint.isStrikeThruText = isChecked
            binding.ListItem.isEnabled = !isChecked

            listItemListener?.onItemCheckedChange(adapterPosition, isChecked)
        }

        binding.ListItem.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                listItemListener?.onItemTextChange(adapterPosition, text.toString())
            }
        })

        binding.DragHandle.setOnTouchListener { v, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                listItemListener?.onStartDrag(this)
            }
            false
        }
    }

    fun bind(item: ListItem) {
        binding.ListItem.setText(item.body)
        binding.CheckBox.isChecked = item.checked
        binding.ListItem.setRawInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
    }

    fun requestFocus() {
        binding.ListItem.requestFocus()
    }
}