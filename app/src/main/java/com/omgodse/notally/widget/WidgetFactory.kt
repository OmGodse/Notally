package com.omgodse.notally.widget

import android.app.Application
import android.content.Intent
import android.os.Build
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.omgodse.notally.R
import com.omgodse.notally.miscellaneous.displayFormattedTimestamp
import com.omgodse.notally.preferences.Preferences
import com.omgodse.notally.preferences.TextSize
import com.omgodse.notally.room.BaseNote
import com.omgodse.notally.room.NotallyDatabase
import com.omgodse.notally.room.Type

class WidgetFactory(private val app: Application, private val id: Long) : RemoteViewsService.RemoteViewsFactory {

    private var baseNote: BaseNote? = null
    private val database = NotallyDatabase.getDatabase(app)
    private val preferences = Preferences.getInstance(app)

    override fun onCreate() {}

    override fun onDestroy() {}


    override fun getCount(): Int {
        val copy = baseNote
        return if (copy != null) {
            when (copy.type) {
                Type.NOTE -> 1
                Type.LIST -> 1 + copy.items.size
            }
        } else 0
    }

    override fun onDataSetChanged() {
        baseNote = database.getBaseNoteDao().get(id)
    }

    override fun getViewAt(position: Int): RemoteViews {
        val copy = baseNote
        requireNotNull(copy)

        return when (copy.type) {
            Type.NOTE -> getNoteView(copy)
            Type.LIST -> {
                if (position > 0) {
                    getListItemView(position - 1, copy)
                } else getListHeaderView(copy)
            }
        }
    }


    private fun getNoteView(note: BaseNote): RemoteViews {
        val view = RemoteViews(app.packageName, R.layout.widget_note)

        view.setTextViewTextSize(R.id.Title, TypedValue.COMPLEX_UNIT_SP, TextSize.getDisplayTitleSize(preferences.textSize.value))
        if (note.title.isNotEmpty()) {
            view.setTextViewText(R.id.Title, note.title)
            view.setViewVisibility(R.id.Title, View.VISIBLE)
        } else view.setViewVisibility(R.id.Title, View.GONE)

        val bodyTextSize = TextSize.getDisplayBodySize(preferences.textSize.value)

        view.setTextViewTextSize(R.id.Date, TypedValue.COMPLEX_UNIT_SP, bodyTextSize)
        view.displayFormattedTimestamp(R.id.Date, note.timestamp, preferences.dateFormat.value)

        view.setTextViewTextSize(R.id.Note, TypedValue.COMPLEX_UNIT_SP, bodyTextSize)
        if (note.body.isNotEmpty()) {
            view.setTextViewText(R.id.Note, note.body)
            view.setViewVisibility(R.id.Note, View.VISIBLE)
        } else view.setViewVisibility(R.id.Note, View.GONE)

        val intent = Intent(WidgetProvider.ACTION_OPEN_NOTE)
        view.setOnClickFillInIntent(R.id.LinearLayout, intent)

        return view
    }


    private fun getListHeaderView(list: BaseNote): RemoteViews {
        val view = RemoteViews(app.packageName, R.layout.widget_list_header)

        view.setTextViewTextSize(R.id.Title, TypedValue.COMPLEX_UNIT_SP, TextSize.getDisplayTitleSize(preferences.textSize.value))
        if (list.title.isNotEmpty()) {
            view.setTextViewText(R.id.Title, list.title)
            view.setViewVisibility(R.id.Title, View.VISIBLE)
        } else view.setViewVisibility(R.id.Title, View.GONE)

        val bodyTextSize = TextSize.getDisplayBodySize(preferences.textSize.value)

        view.setTextViewTextSize(R.id.Date, TypedValue.COMPLEX_UNIT_SP, bodyTextSize)
        view.displayFormattedTimestamp(R.id.Date, list.timestamp, preferences.dateFormat.value)

        val intent = Intent(WidgetProvider.ACTION_OPEN_LIST)
        view.setOnClickFillInIntent(R.id.LinearLayout, intent)

        return view
    }

    private fun getListItemView(index: Int, list: BaseNote): RemoteViews {
        val view = RemoteViews(app.packageName, R.layout.widget_list_item)

        val item = list.items[index]

        view.setTextViewTextSize(R.id.CheckBox, TypedValue.COMPLEX_UNIT_SP, TextSize.getDisplayBodySize(preferences.textSize.value))
        view.setTextViewText(R.id.CheckBox, item.body)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            view.setCompoundButtonChecked(R.id.CheckBox, item.checked)
            val intent = Intent(WidgetProvider.ACTION_CHECKED_CHANGED)
            intent.putExtra(WidgetProvider.EXTRA_POSITION, index)
            val response = RemoteViews.RemoteResponse.fromFillInIntent(intent)
            view.setOnCheckedChangeResponse(R.id.CheckBox, response)
        } else {
            val intent = Intent(WidgetProvider.ACTION_OPEN_LIST)
            if (item.checked) {
                view.setTextViewCompoundDrawablesRelative(R.id.CheckBox, R.drawable.checkbox_fill, 0, 0, 0)
            } else view.setTextViewCompoundDrawablesRelative(R.id.CheckBox, R.drawable.checkbox_outline, 0, 0, 0)
            view.setOnClickFillInIntent(R.id.CheckBox, intent)
        }

        return view
    }


    override fun getViewTypeCount() = 3

    override fun hasStableIds(): Boolean {
        return false
    }

    override fun getLoadingView(): RemoteViews? {
        return null
    }

    override fun getItemId(position: Int): Long {
        return 1
    }
}