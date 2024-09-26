package com.omgodse.notally.recyclerview.viewholder

import android.text.TextWatcher
import android.util.TypedValue
import android.view.KeyEvent
import android.view.MotionEvent
import android.widget.TextView.INVISIBLE
import android.widget.TextView.VISIBLE
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.omgodse.notally.databinding.RecyclerListItemBinding
import com.omgodse.notally.miscellaneous.Change
import com.omgodse.notally.miscellaneous.ChangeHistory
import com.omgodse.notally.miscellaneous.createChangeOnCheckedListener
import com.omgodse.notally.miscellaneous.createListChangeTextWatcher
import com.omgodse.notally.miscellaneous.setOnNextAction
import com.omgodse.notally.preferences.ListItemSorting
import com.omgodse.notally.preferences.TextSize
import com.omgodse.notally.recyclerview.ListItemListener
import com.omgodse.notally.room.ListItem
import com.zerobranch.layout.SwipeLayout.SwipeActionsListener


class MakeListVH(
    val binding: RecyclerListItemBinding,
    val listener: ListItemListener,
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
            listener.add(position, children = mutableListOf())
            changeHistory.addChange(object : Change {
                override fun redo() {
                    listener.add(position, children = mutableListOf())
                }

                override fun undo() {
                    listener.delete(position, true)
                }

                override fun toString(): String {
                    return "Add at position: $position"
                }

            })
        }

        editTextWatcher = binding.EditText.createListChangeTextWatcher(
            changeHistory,
            { adapterPosition }
        ) { position, text ->
            listener.textChanged(position, text)
        }
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

        updateSwipe(item.isChildItem, firstItem)
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
            val text = binding.EditText.text.toString()
            val checked = binding.CheckBox.isChecked
            val isChildItem = binding.SwipeLayout.isLeftOpen
            listener.delete(adapterPosition, true)
            changeHistory.addChange(object : Change {
                override fun redo() {
                    listener.delete(positionBeforeDelete, true)
                }

                override fun undo() {
                    listener.add(
                        positionBeforeDelete,
                        text,
                        checked,
                        isChildItem,
                        item.uncheckedPosition,
                        mutableListOf() // TODO make listener.delete return removed object
                    )
                }

                override fun toString(): String {
                    return "DeleteChange at $positionBeforeDelete"
                }
            })
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
                if (listener.delete(adapterPosition, false)) {
                    val text = binding.EditText.text.toString()
                    val checked = binding.CheckBox.isChecked
                    changeHistory.addChange(object : Change {
                        override fun redo() {
                            listener.delete(positionBeforeDelete, true)
                        }

                        override fun undo() {
                            listener.add(
                                positionBeforeDelete,
                                text,
                                checked,
                                item.isChildItem,
                                item.uncheckedPosition,
                                mutableListOf() // TODO make listener.delete return removed object
                            )
                        }

                        override fun toString(): String {
                            return "DeleteChange at $positionBeforeDelete"
                        }

                    })
                }
            }
            true
        }
    }

    private fun updateCheckBox(item: ListItem) {
        binding.CheckBox.setOnCheckedChangeListener(null)
        binding.CheckBox.isChecked = item.checked
        binding.CheckBox.setOnCheckedChangeListener(createChangeOnCheckedListener(changeHistory) { position, isChecked ->
            listener.checkedChanged(position, isChecked)
        })
    }

    private fun updateSwipe(open: Boolean, firstItem: Boolean) {
        binding.SwipeLayout.setOnActionsListener(null)
        val swipeActionListener = object : SwipeActionsListener {
            override fun onOpen(direction: Int, isContinuous: Boolean) {
                val position = adapterPosition
                listener.isChildItemChanged(position, true)
                changeHistory.addChange(object : Change {
                    override fun redo() {
                        listener.isChildItemChanged(position, true)
                        updateSwipe(true, firstItem)
                    }

                    override fun undo() {
                        listener.isChildItemChanged(position, false)
                        updateSwipe(false, firstItem)
                    }

                    override fun toString(): String {
                        return "SwipeChange at $position true"
                    }
                })
            }

            override fun onClose() {
                val position = adapterPosition
                listener.isChildItemChanged(adapterPosition, false)
                changeHistory.addChange(object : Change {
                    override fun redo() {
                        listener.isChildItemChanged(position, false)
                        updateSwipe(false, firstItem)
                    }

                    override fun undo() {
                        listener.isChildItemChanged(position, true)
                        updateSwipe(true, firstItem)
                    }

                    override fun toString(): String {
                        return "SwipeChange at $position false"
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