package com.omgodse.notally.adapters

import android.content.Context
import android.text.Editable
import android.text.InputType
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.checkbox.MaterialCheckBox
import com.google.android.material.textfield.TextInputEditText
import com.omgodse.notally.R
import com.omgodse.notally.interfaces.ListItemListener
import com.omgodse.notally.miscellaneous.setOnNextAction
import com.omgodse.notally.xml.ListItem
import java.util.*

class MakeListAdapter(private val context: Context, var items: ArrayList<ListItem>) :
    RecyclerView.Adapter<MakeListAdapter.ViewHolder>() {

    var listItemListener: ListItemListener? = null

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val listItem = items[position]
        holder.listItem.setText(listItem.body)
        holder.checkBox.isChecked = listItem.checked
        holder.listItem.setRawInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.list_item, parent, false)
        return ViewHolder(view)
    }


    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val listItem: TextInputEditText = view.findViewById(R.id.ListItem)
        val checkBox: MaterialCheckBox = view.findViewById(R.id.CheckBox)
        private val dragHandle: ImageView = view.findViewById(R.id.DragHandle)

        init {
            listItem.setOnNextAction {
                listItemListener?.onMoveToNext(adapterPosition)
            }

            checkBox.setOnCheckedChangeListener { buttonView, isChecked ->
                listItem.paint.isStrikeThruText = isChecked
                listItem.isEnabled = !isChecked

                listItemListener?.onItemCheckedChange(adapterPosition, isChecked)
            }

            listItem.addTextChangedListener(object : TextWatcher {
                override fun afterTextChanged(s: Editable?) {}

                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

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