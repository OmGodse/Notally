package com.omgodse.notally.recyclerview.viewholder

import android.text.TextWatcher
import android.util.TypedValue
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.TextView.INVISIBLE
import android.widget.TextView.VISIBLE
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.omgodse.notally.changehistory.ListAddChange
import com.omgodse.notally.changehistory.ListBooleanChange
import com.omgodse.notally.databinding.RecyclerListItemBinding
import com.omgodse.notally.changehistory.ChangeHistory
import com.omgodse.notally.changehistory.ListDeleteChange
import com.omgodse.notally.miscellaneous.createListTextWatcherWithHistory
import com.omgodse.notally.miscellaneous.setOnCheckedChangeListenerWithHistory
import com.omgodse.notally.miscellaneous.setOnNextAction
import com.omgodse.notally.preferences.ListItemSorting
import com.omgodse.notally.preferences.TextSize
import com.omgodse.notally.recyclerview.ListManager
import com.omgodse.notally.room.ListItem
import com.zerobranch.layout.SwipeLayout.SwipeActionsListener


class MakeListVH(
    val binding: RecyclerListItemBinding,
    val listManager: ListManager,
    val changeHistory: ChangeHistory,
    touchHelper: ItemTouchHelper,
    textSize: String
) : RecyclerView.ViewHolder(binding.root) {

    private var editTextWatcher: TextWatcher

    init {
        val body = TextSize.getEditBodySize(textSize)
        binding.EditText.setTextSize(TypedValue.COMPLEX_UNIT_SP, body)

        binding.EditText.setOnNextAction {
            val position = adapterPosition + 1
            listManager.add(position)
            changeHistory.push(ListAddChange(position, listManager))
        }

        editTextWatcher = binding.EditText.createListTextWatcherWithHistory(
            changeHistory,
            listManager,
            this::getAdapterPosition
        )
        binding.EditText.addTextChangedListener(editTextWatcher)

        binding.EditText.setOnFocusChangeListener { _, hasFocus ->
            binding.Delete.visibility = if (hasFocus) VISIBLE else INVISIBLE
        }

        binding.DragHandle.setOnTouchListener { _, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                touchHelper.startDrag(this)
            }
            false
        }

    }

    fun bind(item: ListItem, firstItem: Boolean, autoSort: String) {
        updateEditText(item)

        updateCheckBox(item)

        updateDeleteButton(item)

        updateSwipe(item.isChild, firstItem)
        if (item.checked && autoSort == ListItemSorting.autoSortByChecked) {
            binding.DragHandle.visibility = INVISIBLE
        } else {
            binding.DragHandle.visibility = VISIBLE
        }
    }

    private fun updateDeleteButton(item: ListItem) {
        binding.Delete.visibility = if (item.checked) VISIBLE else INVISIBLE
        binding.Delete.setOnClickListener {
            val positionBeforeDelete = adapterPosition
            val deletedItem = listManager.delete(positionBeforeDelete, true)!!
            changeHistory.push(ListDeleteChange(positionBeforeDelete, deletedItem, listManager))
        }
    }

    private fun updateEditText(item: ListItem) {
        binding.EditText.removeTextChangedListener(editTextWatcher)
        binding.EditText.setText(item.body)
        binding.EditText.isEnabled = !item.checked
        binding.EditText.addTextChangedListener(editTextWatcher)
        binding.EditText.setOnKeyListener { _, keyCode, event ->
            if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_DEL) {
                val positionBeforeDelete = adapterPosition
                val deletedItem = listManager.delete(adapterPosition, false)
                if (deletedItem != null) {
                    changeHistory.push(
                        ListDeleteChange(
                            positionBeforeDelete,
                            deletedItem,
                            listManager
                        )
                    )
                }
            }
            true
        }
    }

    private fun updateCheckBox(item: ListItem) {
        binding.CheckBox.setOnCheckedChangeListener(null)
        binding.CheckBox.isChecked = item.checked
        binding.CheckBox.setOnCheckedChangeListenerWithHistory(
            this::getAdapterPosition,
            changeHistory
        ) { position, isChecked ->
            listManager.changeChecked(position, isChecked)
        }
    }

    private fun updateSwipe(open: Boolean, firstItem: Boolean) {
        binding.SwipeLayout.setOnActionsListener(null)
        val swipeActionListener = object : SwipeActionsListener {
            override fun onOpen(direction: Int, isContinuous: Boolean) {
                val position = adapterPosition
                listManager.changeIsChild(position, true)

                changeHistory.push(object: ListBooleanChange(true, position) {
                    override fun update(position: Int, value: Boolean, isUndo: Boolean) {
                        listManager.changeIsChild(position, value)
                        updateSwipe(value, firstItem)
                    }
                })
            }

            override fun onClose() {
                val position = adapterPosition
                listManager.changeIsChild(position, false)
                changeHistory.push(object: ListBooleanChange(false, position) {
                    override fun update(position: Int, value: Boolean, isUndo: Boolean) {
                        listManager.changeIsChild(position, value)
                        updateSwipe(value, firstItem)
                    }
                })
            }
        }
        binding.SwipeLayout.isEnabledSwipe = !firstItem

        binding.SwipeLayout.post {
            if (open) {
                binding.SwipeLayout.openLeft(false)
            } else {
                binding.SwipeLayout.close(false)
            }
            binding.SwipeLayout.setOnActionsListener(swipeActionListener)
        }

    }

}