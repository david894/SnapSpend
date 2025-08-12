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
            val views = RemoteViews(context.packageName, R.layout.collection_widget)

            // Set the background color (this is your existing code)
            views.setInt(R.id.widget_root, "setBackgroundColor",
                context.resources.getColor(R.color.widget_background_color, null)
            )

            // ** THIS IS THE FIX **
            // Programmatically set the text color using our new theme-aware color.
            views.setTextColor(R.id.widget_title,
                context.resources.getColor(R.color.widget_text_color, null)
            )

            // The rest of the function remains the same...
            val intent = Intent(context, WidgetService::class.java).apply {
                putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
                data = Uri.parse(toUri(Intent.URI_INTENT_SCHEME))
            }
            views.setRemoteAdapter(R.id.widget_grid_view, intent)

            val clickIntent = Intent(context, WidgetEntryActivity::class.java)
            val clickPendingIntent = PendingIntent.getActivity(
                context, 0, clickIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )
            views.setPendingIntentTemplate(R.id.widget_grid_view, clickPendingIntent)

            appWidgetManager.updateAppWidget(appWidgetId, views)
            appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.widget_grid_view)
        }
    }
}