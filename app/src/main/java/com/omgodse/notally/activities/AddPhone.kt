package com.omgodse.notally.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.omgodse.notally.databinding.ActivityAddPhoneBinding
import com.omgodse.notally.miscellaneous.bindLabels
import com.omgodse.notally.miscellaneous.getLocale
import com.omgodse.notally.miscellaneous.setOnNextAction
import com.omgodse.notally.recyclerview.PhoneItemListener
import com.omgodse.notally.recyclerview.adapters.PhoneAdapter
import com.omgodse.notally.recyclerview.viewholders.AddPhoneVH
import com.omgodse.notally.room.PhoneItem
import com.omgodse.notally.room.Type
import com.omgodse.notally.viewmodels.BaseNoteModel
import java.util.*

class AddPhone : NotallyActivity() {

    private lateinit var adapter: PhoneAdapter

    override val type = Type.PHONE
    override val binding by lazy { ActivityAddPhoneBinding.inflate(layoutInflater) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.EnterTitle.setOnNextAction {
            moveToNext(-1)
        }

        setupListeners()
        setupRecyclerView()
        setupToolbar(binding.Toolbar)

        if (model.isNewNote && model.phoneItems.isEmpty()) {
            addPhoneItem()
        }

        binding.AddItem.setOnClickListener {
            addPhoneItem()
        }

        setStateFromModel()
    }


    override fun getLabelGroup() = binding.LabelGroup


    private fun addPhoneItem() {
        val position = model.phoneItems.size
        val phoneItem = PhoneItem()
        model.phoneItems.add(phoneItem)
        adapter.notifyItemInserted(position)
        binding.RecyclerView.post {
            val viewHolder =
                binding.RecyclerView.findViewHolderForAdapterPosition(position) as AddPhoneVH?
            viewHolder?.binding?.contactName?.requestFocus()
        }
    }

    private fun setupListeners() {
        binding.EnterTitle.doAfterTextChanged { text -> model.title = text.toString().trim() }
    }


    private fun setupRecyclerView() {
        val itemTouchHelper = ItemTouchHelper(object : ItemTouchHelper.Callback() {

            override fun getMovementFlags(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder
            ): Int {
                val drag = ItemTouchHelper.UP or ItemTouchHelper.DOWN
                val swipe = ItemTouchHelper.START or ItemTouchHelper.END
                return makeMovementFlags(drag, swipe)
            }

            override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
                model.phoneItems.removeAt(viewHolder.adapterPosition)
                adapter.notifyItemRemoved(viewHolder.adapterPosition)
            }

            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean {
                Collections.swap(
                    model.phoneItems,
                    viewHolder.adapterPosition,
                    target.adapterPosition
                )
                adapter.notifyItemMoved(viewHolder.adapterPosition, target.adapterPosition)
                return true
            }
        })

        adapter = PhoneAdapter(model.phoneItems, object : PhoneItemListener {

            override fun onMoveToNext(position: Int) {
                moveToNext(position)
            }

            override fun onStartDrag(viewHolder: RecyclerView.ViewHolder) {
                itemTouchHelper.startDrag(viewHolder)
            }

            override fun afterContactChanged(position: Int, text: String) {
                model.phoneItems[position].contactName = text
            }

            override fun afterNumberChanged(position: Int, text: String) {
                model.phoneItems[position].contactNo = text
            }

            override fun callPhoneNumber(number: String) {
                startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:$number")))
            }
        })

        itemTouchHelper.attachToRecyclerView(binding.RecyclerView)

        binding.RecyclerView.adapter = adapter
        binding.RecyclerView.layoutManager = LinearLayoutManager(this)
    }

    private fun setStateFromModel() {
        val formatter = BaseNoteModel.getDateFormatter(getLocale())

        binding.EnterTitle.setText(model.title)
        binding.DateCreated.text = formatter.format(model.timestamp)

        binding.LabelGroup.bindLabels(model.labels)
    }

    private fun moveToNext(currentPosition: Int) {
        val viewHolder =
            binding.RecyclerView.findViewHolderForAdapterPosition(currentPosition + 1) as AddPhoneVH?
        if (viewHolder != null) {
            viewHolder.binding.contactName.requestFocus()
        } else addPhoneItem()
    }
}