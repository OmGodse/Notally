package com.omgodse.notally.widget

import android.app.Application
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.omgodse.notally.R
import com.omgodse.notally.room.BaseNote
import com.omgodse.notally.room.NotallyDatabase
import com.omgodse.notally.viewmodels.BaseNoteModel

class WidgetFactory(private val context: Context, private val intent: Intent) : RemoteViewsService.RemoteViewsFactory {

    private var note: BaseNote? = null

    override fun onCreate() {}

    override fun onDestroy() {}


    override fun getCount(): Int {
        return if (note != null) 1 else 0
    }

    override fun onDataSetChanged() {
        val id = intent.getLongExtra(WidgetProvider.EXTRA_NOTE_ID, 0)
        val database = NotallyDatabase.getDatabase(context.applicationContext as Application)
        note = database.baseNoteDao.get(id)
    }

    override fun getViewAt(position: Int): RemoteViews {
        val view = RemoteViews(context.packageName, R.layout.widget_item)

        val copy = note

        if (copy != null) {
            val formatter = BaseNoteModel.getDateFormatter(context)
            val date = formatter.format(copy.timestamp)

            if (copy.title.isNotEmpty()) {
                view.setTextViewText(R.id.Title, copy.title)
                view.setViewVisibility(R.id.Title, View.VISIBLE)
            } else view.setViewVisibility(R.id.Title, View.GONE)

            view.setTextViewText(R.id.Date, date)

            if (copy.body.isNotEmpty()) {
                view.setTextViewText(R.id.Note, copy.body)
                view.setViewVisibility(R.id.Note, View.VISIBLE)
            } else view.setViewVisibility(R.id.Note, View.GONE)

            view.setOnClickFillInIntent(R.id.LinearLayout, Intent())
        }

        return view
    }


    override fun hasStableIds(): Boolean {
        return false
    }

    override fun getViewTypeCount(): Int {
        return 1
    }

    override fun getLoadingView(): RemoteViews? {
        return null
    }

    override fun getItemId(position: Int): Long {
        return 1
    }
}