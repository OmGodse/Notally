package com.omgodse.notally.recyclerview

import androidx.recyclerview.widget.RecyclerView

interface PhoneItemListener {

    fun onMoveToNext(position: Int)

    fun onStartDrag(viewHolder: RecyclerView.ViewHolder)

    fun afterContactChanged(position: Int, text: String)

    fun afterNumberChanged(position: Int, text: String)

    fun callPhoneNumber(number: String)

}