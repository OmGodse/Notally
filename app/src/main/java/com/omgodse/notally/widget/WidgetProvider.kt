package com.omgodse.notally.widget

import android.app.Application
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.omgodse.notally.R
import com.omgodse.notally.activities.ConfigureWidget
import com.omgodse.notally.activities.TakeNote
import com.omgodse.notally.miscellaneous.Constants
import com.omgodse.notally.preferences.Preferences

class WidgetProvider : AppWidgetProvider() {

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_NOTE_MODIFIED) {
            val noteId = intent.getLongExtra(EXTRA_NOTE_ID, 0)

            val app = context.applicationContext as Application
            val preferences = Preferences.getInstance(app)

            val manager = AppWidgetManager.getInstance(context)
            val widgetIds = preferences.getWidgetIds(noteId)
            widgetIds.forEach { id ->
                updateWidget(context, manager, id, noteId)
            }
        }
    }


    override fun onDeleted(context: Context, appWidgetIds: IntArray) {
        val app = context.applicationContext as Application
        val preferences = Preferences.getInstance(app)

        appWidgetIds.forEach { id -> preferences.deleteWidget(id) }
    }

    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        val app = context.applicationContext as Application
        val preferences = Preferences.getInstance(app)

        appWidgetIds.forEach { id ->
            val noteId = preferences.getWidgetData(id)
            updateWidget(context, appWidgetManager, id, noteId)
        }
    }

    companion object {

        fun updateWidget(context: Context, manager: AppWidgetManager, id: Int, noteId: Long) {
            // Widgets displaying the same note share the same factory since only the noteId is embedded
            val intent = Intent(context, WidgetService::class.java)
            intent.putExtra(EXTRA_NOTE_ID, noteId)
            embedIntentExtras(intent)

            val view = RemoteViews(context.packageName, R.layout.widget)
            view.setRemoteAdapter(R.id.ListView, intent)
            view.setEmptyView(R.id.ListView, R.id.Empty)

            val selectNote = getSelectNoteIntent(context, id)
            view.setOnClickPendingIntent(R.id.Empty, selectNote)

            val openNote = getOpenNoteIntent(context, noteId)
            view.setPendingIntentTemplate(R.id.ListView, openNote)

            manager.updateAppWidget(id, view)
            manager.notifyAppWidgetViewDataChanged(id, R.id.ListView)
        }

        // Each widget has it's own intent since the widget id is embedded
        private fun getSelectNoteIntent(context: Context, id: Int): PendingIntent {
            val intent = Intent(context, ConfigureWidget::class.java)
            intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
            embedIntentExtras(intent)
            val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            return PendingIntent.getActivity(context, 0, intent, flags)
        }

        private fun getOpenNoteIntent(context: Context, noteId: Long): PendingIntent {
            val intent = Intent(context, TakeNote::class.java)
            intent.putExtra(Constants.SelectedBaseNote, noteId)
            embedIntentExtras(intent)
            val flags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            return PendingIntent.getActivity(context, 0, intent, flags)
        }

        private fun embedIntentExtras(intent: Intent) {
            val string = intent.toUri(Intent.URI_INTENT_SCHEME)
            intent.data = Uri.parse(string)
        }

        const val EXTRA_NOTE_ID = "com.omgodse.notally.EXTRA_NOTE_ID"
        const val ACTION_NOTE_MODIFIED = "com.omgodse.notally.ACTION_NOTE_MODIFIED"
    }
}