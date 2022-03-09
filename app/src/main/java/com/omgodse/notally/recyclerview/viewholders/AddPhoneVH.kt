package com.omgodse.notally.recyclerview.viewholders

import android.telephony.PhoneNumberFormattingTextWatcher
import android.text.Editable
import android.text.InputType
import android.view.MotionEvent
import androidx.core.widget.doAfterTextChanged
import androidx.recyclerview.widget.RecyclerView
import com.omgodse.notally.databinding.RecyclerPhoneItemBinding
import com.omgodse.notally.miscellaneous.setOnNextAction
import com.omgodse.notally.recyclerview.PhoneItemListener
import com.omgodse.notally.room.PhoneItem

class AddPhoneVH(val binding: RecyclerPhoneItemBinding, listener: PhoneItemListener) :
    RecyclerView.ViewHolder(binding.root) {

    init {
        binding.contactName.setOnNextAction {
            binding.phoneNumber.requestFocus()
        }

        binding.phoneNumber.setOnNextAction {
            listener.onMoveToNext(adapterPosition)
        }

        binding.contactName.doAfterTextChanged { text ->
            listener.afterContactChanged(adapterPosition, text.toString().trim())
        }

        binding.phoneNumber.doAfterTextChanged { text ->
            listener.afterNumberChanged(adapterPosition, text.toString().trim())
        }

        binding.phoneNumber.addTextChangedListener(PhoneNumberFormattingTextWatcher("US"))

        binding.phoneIv.setOnClickListener {
            sanitisePhoneNumber(binding.phoneNumber.text)?.let {
                listener.callPhoneNumber(it)
            }
        }

        binding.DragHandle.setOnTouchListener { v, event ->
            if (event.actionMasked == MotionEvent.ACTION_DOWN) {
                listener.onStartDrag(this)
            }
            false
        }
    }

    private fun sanitisePhoneNumber(editText: Editable?): String? {
        return editText?.toString()
    }

    fun bind(item: PhoneItem) {
        binding.contactName.setText(item.contactName)
        binding.phoneNumber.setText(item.contactNo)
        binding.contactName.setRawInputType(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
    }
}