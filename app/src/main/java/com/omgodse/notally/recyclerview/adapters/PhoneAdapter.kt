package com.omgodse.notally.recyclerview.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.omgodse.notally.databinding.RecyclerPhoneItemBinding
import com.omgodse.notally.recyclerview.PhoneItemListener
import com.omgodse.notally.recyclerview.viewholders.AddPhoneVH
import com.omgodse.notally.room.PhoneItem
import java.util.*

class PhoneAdapter(private val items: ArrayList<PhoneItem>, private val listener: PhoneItemListener) :
    RecyclerView.Adapter<AddPhoneVH>() {

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: AddPhoneVH, position: Int) {
        val item = items[position]
        holder.bind(item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AddPhoneVH {
        val inflater = LayoutInflater.from(parent.context)
        val binding = RecyclerPhoneItemBinding.inflate(inflater, parent, false)
        return AddPhoneVH(binding, listener)
    }
}