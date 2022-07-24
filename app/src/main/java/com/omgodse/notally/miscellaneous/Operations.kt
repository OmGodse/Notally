package com.omgodse.notally.miscellaneous

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.chip.ChipGroup
import com.omgodse.notally.R
import com.omgodse.notally.databinding.LabelBinding
import com.omgodse.notally.room.Color
import com.omgodse.notally.room.ListItem

object Operations {

    const val extraCharSequence = "com.omgodse.notally.extra.charSequence"

    fun extractColor(color: Color, context: Context): Int {
        val id = when (color) {
            Color.DEFAULT -> R.color.Default
            Color.CORAL -> R.color.Coral
            Color.ORANGE -> R.color.Orange
            Color.SAND -> R.color.Sand
            Color.STORM -> R.color.Storm
            Color.FOG -> R.color.Fog
            Color.SAGE -> R.color.Sage
            Color.MINT -> R.color.Mint
            Color.DUSK -> R.color.Dusk
            Color.FLOWER -> R.color.Flower
            Color.BLOSSOM -> R.color.Blossom
            Color.CLAY -> R.color.Clay
        }
        return ContextCompat.getColor(context, id)
    }


    fun shareNote(context: Context, title: String, body: CharSequence) {
        val text = body.toString()
        val intent = Intent(Intent.ACTION_SEND)
        intent.type = "text/plain"
        intent.putExtra(extraCharSequence, body)
        intent.putExtra(Intent.EXTRA_TEXT, text)
        intent.putExtra(Intent.EXTRA_SUBJECT, title)
        val label = context.getString(R.string.share_note)
        val chooser = Intent.createChooser(intent, label)
        context.startActivity(chooser)
    }


    fun getBody(list: List<ListItem>) = buildString {
        list.forEachIndexed { index, item ->
            appendLine("${index + 1}) ${item.body}")
        }
    }

    fun bindLabels(group: ChipGroup, labels: HashSet<String>) {
        if (labels.isEmpty()) {
            group.visibility = View.GONE
        } else {
            group.visibility = View.VISIBLE
            group.removeAllViews()
            val inflater = LayoutInflater.from(group.context)
            for (label in labels) {
                val view = LabelBinding.inflate(inflater, group, true).root
                view.text = label
            }
        }
    }
}