package com.omgodse.notally.room

import androidx.room.Ignore

sealed class Item(@Ignore val viewType: ViewType)