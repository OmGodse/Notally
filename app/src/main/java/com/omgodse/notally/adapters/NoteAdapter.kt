package com.omgodse.notally.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textview.MaterialTextView
import com.omgodse.notally.R
import com.omgodse.notally.helpers.SettingsHelper
import com.omgodse.notally.interfaces.NoteListener
import com.omgodse.notally.miscellaneous.applySpans
import com.omgodse.notally.viewholders.NoteHolder
import com.omgodse.notally.xml.XMLReader
import org.ocpsoft.prettytime.PrettyTime
import java.io.File
import java.util.*

class NoteAdapter(private val context: Context, var files: ArrayList<File>) :
    RecyclerView.Adapter<NoteHolder>() {

    var noteListener: NoteListener? = null

    override fun getItemCount(): Int {
        return files.size
    }

    override fun onBindViewHolder(holder: NoteHolder, position: Int) {
        val file = files[position]

        if (XMLReader(file).isNote()) {
            setupTextNote(file, holder)
        } else setupListNote(file, holder)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NoteHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.recycler_view_item, parent, false)
        return NoteHolder(context, view, noteListener)
    }


    private fun setupTextNote(file: File, holder: NoteHolder) {
        holder.labelGroup.removeAllViews()
        holder.displayList.visibility = View.GONE
        holder.listItemsRemaining.visibility = View.GONE

        val xmlReader = XMLReader(file)
        val body = xmlReader.getBody()
        val spans = xmlReader.getSpans()
        val title = xmlReader.getTitle()
        val labels = xmlReader.getLabels()
        val dateCreated = xmlReader.getDateCreated()

        holder.displayTitle.text = title
        holder.displayBody.text = body.applySpans(spans)
        holder.displayDate.text = PrettyTime().format(Date(dateCreated.toLong()))

        if (title.isEmpty()) {
            holder.displayTitle.visibility = View.GONE
        } else holder.displayTitle.visibility = View.VISIBLE

        if (body.isEmpty()) {
            holder.displayBody.visibility = View.GONE
        } else holder.displayBody.visibility = View.VISIBLE

        for (label in labels) {
            val labelChip = View.inflate(context, R.layout.chip_label, null) as MaterialButton
            labelChip.text = label
            holder.labelGroup.addView(labelChip)
        }
    }

    private fun setupListNote(file: File, holder: NoteHolder) {
        holder.labelGroup.removeAllViews()
        holder.displayList.removeAllViews()

        holder.displayBody.visibility = View.GONE
        holder.displayList.visibility = View.VISIBLE

        val xmlReader = XMLReader(file)
        val title = xmlReader.getTitle()
        val items = xmlReader.getListItems()
        val labels = xmlReader.getLabels()
        val dateCreated = xmlReader.getDateCreated()

        holder.displayTitle.text = title
        holder.displayDate.text = PrettyTime().format(Date(dateCreated.toLong()))

        if (title.isEmpty()) {
            holder.displayTitle.visibility = View.GONE
        } else holder.displayTitle.visibility = View.VISIBLE

        val settingsHelper = SettingsHelper(context)

        val limit = try {
            settingsHelper.getMaxItemsPreference().toInt()
        } catch (exception: Exception) {
            4
        }

        val filteredList = if (items.size >= limit) {
            items.subList(0, limit)
        } else items

        holder.listItemsRemaining.visibility = if (items.size > limit) {
            View.VISIBLE
        } else View.GONE

        holder.listItemsRemaining.text = if (items.size > limit) {
            val itemsRemaining = items.size - limit
            if (itemsRemaining == 1) {
                "$itemsRemaining ${context.getString(R.string.more_item)}"
            } else "$itemsRemaining ${context.getString(R.string.more_items)}"
        } else null

        for (listItem in filteredList) {
            val view = View.inflate(context, R.layout.preview_item, null) as MaterialTextView
            view.text = listItem.body
            handleChecked(view, listItem.checked)
            holder.displayList.addView(view)
        }

        for (label in labels) {
            val labelChip = View.inflate(context, R.layout.chip_label, null) as MaterialButton
            labelChip.text = label
            holder.labelGroup.addView(labelChip)
        }
    }

    private fun handleChecked(textView: MaterialTextView, checked: Boolean) {
        if (checked) {
            textView.paint.isStrikeThruText = true
            textView.setCompoundDrawablesRelativeWithIntrinsicBounds(R.drawable.checkbox_16, 0, 0, 0)
        } else {
            textView.paint.isStrikeThruText = false
            textView.setCompoundDrawablesWithIntrinsicBounds(R.drawable.checkbox_outline_16, 0, 0, 0)
        }
    }
}