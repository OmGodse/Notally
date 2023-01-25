package com.omgodse.notally.preferences

import com.omgodse.notally.R

sealed interface SwitchInfo {

    val title: Int
    val subtitle: Int

    val key: String
    val defaultValue: Boolean
}

object AutoBackup : SwitchInfo {

    override val title = R.string.auto_backup
    override val subtitle = R.string.notes_will_be_backed

    override val key = "autoBackup"
    override val defaultValue = true
}