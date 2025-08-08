package com.kx.snapspend.widget

import android.content.Intent
import android.widget.RemoteViewsService

class WidgetService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return CollectionRemoteViewsFactory(this.applicationContext, intent)
    }
}