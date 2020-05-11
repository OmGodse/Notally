package com.omgodse.notally.adapters

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.EditorInfo
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.omgodse.notally.R
import com.omgodse.notally.interfaces.ListItemListener
import com.omgodse.notally.miscellaneous.ListItem
import java.util.*

class MakeListAdapter(private val context: Context, var items: ArrayList<ListItem>) :
    RecyclerView.Adapter<MakeListAdapter.ListHolder>() {

    var listItemListener: ListItemListener? = null

    fun onItemDismiss(position: Int) {
        items.removeAt(position)
        notifyItemRemoved(position)
        notifyItemRangeChanged(position, items.size)
    }

    fun onItemMove(fromPosition: Int, toPosition: Int): Boolean {
        Collections.swap(items, fromPosition, toPosition)
        notifyItemMoved(fromPosition, toPosition)
        return true
    }


    override fun getItemCount(): Int {
        return items.size
    }

    override fun onBindViewHolder(holder: ListHolder, position: Int) {
        val listItem = items[position]
        holder.listItem.setText(listItem.body)
        holder.checkBox.isChecked = listItem.checked
        holder.listItem.setRawInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.list_item, parent, false)
        return ListHolder(view)
    }


    inner class ListHolder(view: View) : RecyclerView.ViewHolder(view) {

        val listItem: TextInputEditText = view.findViewById(R.id.ListItem)
        val checkBox: MaterialCheckBox = view.findViewById(R.id.CheckBox)
        private val dragHandle: ImageView = view.findViewById(R.id.DragHandle)

        init {
            listItem.setOnKeyListener { v, keyCode, event ->
                if (event.action == KeyEvent.ACTION_DOWN && keyCode == KeyEvent.KEYCODE_ENTER) {
                    listItemListener?.onMoveToNext(adapterPosition)
                    return@setOnKeyListener true
                }
                return@setOnKeyListener false
            }

            listItem.setOnEditorActionListener { v, actionId, event ->
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    listItemListener?.onMoveToNext(adapterPosition)
                    return@setOnEditorActionListener true
                }
                return@setOnEditorActionListener false
            }

            checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked) {
                    listItem.paint.isStrikeThruText = true
                    listItem.isEnabled = false
                } else {
                    listItem.paint.isStrikeThruText = false
                    listItem.isEnabled = true
                }
                listItemListener?.onItemCheckedChange(adapterPosition, isChecked)
            }

            listItem.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {}

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
                }

                override fun onTextChanged(text: CharSequence?, start: Int, before: Int, count: Int) {
                    listItemListener?.onItemTextChange(adapterPosition, text.toString())
                }
            })

            dragHandle.setOnTouchListener { v, event ->
                if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                    listItemListener?.onStartDrag(this)
                }
                false
            }
        }
    }
}