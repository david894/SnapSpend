package com.kx.snapspend.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import com.kx.snapspend.R
import com.kx.snapspend.ui.WidgetEntryActivity

class CollectionWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        for (appWidgetId in appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId)
        }
    }

    // In widget/CollectionWidgetProvider.kt
    companion object {
        internal fun updateAppWidget(
            context: Context,
            appWidgetManager: AppWidgetManager,
            appWidgetId: Int
        ) {
            // Set up the intent that starts the WidgetService
            val intent = Intent(context, WidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }

            val views = RemoteViews(context.packageName, R.layout.collection_widget)
            views.setRemoteAdapter(R.id.widget_grid_view, intent)

            // Set up the pending intent template for the GridView items
            val clickIntent = Intent(context, WidgetEntryActivity::class.java)
            val clickPendingIntent = PendingIntent.getActivity(
                context, 0, clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_grid_view, clickPendingIntent)

            // Finally, update the widget
            appWidgetManager.updateAppWidget(appWidgetId, views)
            // And notify the grid view to refresh its data
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_grid_view)
        }
    }
}