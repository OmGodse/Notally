package com.omgodse.notally.changehistory

abstract class ListBooleanChange(
    newValue: Boolean,
    position: Int,
    positionAfter: Int = position
) : ListValueChange<Boolean>(newValue, !newValue, position, positionAfter)