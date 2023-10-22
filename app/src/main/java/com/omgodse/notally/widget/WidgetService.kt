package com.omgodse.notally.widget

import android.content.Intent
import android.widget.RemoteViewsService
import com.omgodse.notally.miscellaneous.Constants

class WidgetService : RemoteViewsService() {

    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        val id = intent.getLongExtra(Constants.SelectedBaseNote, 0)
        return WidgetFactory(application, id)
    }
}