package com.omgodse.notally.room

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize

@Entity
@Parcelize
data class Label(@PrimaryKey val value: String) : Parcelable