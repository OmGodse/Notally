package com.omgodse.notally.preferences

import com.omgodse.notally.R

sealed interface SeekbarInfo {

    val title: Int

    val key: String
    val defaultValue: Int

    val min: Int
    val max: Int
}

object MaxItems : SeekbarInfo {

    override val title = R.string.max_items_to_display

    override val key = "maxItemsToDisplayInList.v1"
    override val defaultValue = 4

    override val min = 1
    override val max = 10
}

object MaxLines : SeekbarInfo {

    override val title = R.string.max_lines_to_display

    override val key = "maxLinesToDisplayInNote.v1"
    override val defaultValue = 8

    override val min = 1
    override val max = 10
}

object MaxTitle : SeekbarInfo {

    override val title = R.string.max_lines_to_display_title

    override val key = "maxLinesToDisplayInTitle"
    override val defaultValue = 1

    override val min = 1
    override val max = 10
}