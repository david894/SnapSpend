// In file: widget/CollectionRemoteViewsFactory.kt
package com.kx.snapspend.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.RemoteViews
import android.widget.RemoteViewsService
import com.kx.snapspend.BudgetTrackerApplication
import com.kx.snapspend.R
import com.kx.snapspend.model.Collections
import com.kx.snapspend.ui.CollectionWidgetConfigureActivity
import com.kx.snapspend.ui.WidgetEntryActivity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class CollectionRemoteViewsFactory(
    private val context: Context,
    private val intent: Intent
) : RemoteViewsService.RemoteViewsFactory {

    private var collections: List<Collections> = emptyList()
    private val appWidgetId = intent.getIntExtra(
        AppWidgetManager.EXTRA_APPWIDGET_ID,
        AppWidgetManager.INVALID_APPWIDGET_ID
    )

    override fun onCreate() {
        // Nothing to do
    }

    // ** THIS IS THE OPTIMIZED METHOD **
    override fun onDataSetChanged() {
        // Use runBlocking to bridge the synchronous world of widgets
        // with the asynchronous world of coroutines.
        runBlocking {
            // Explicitly run the database query on the IO dispatcher,
            // which is optimized for blocking disk and network operations.
            withContext(Dispatchers.IO) {
                val repository = (context.applicationContext as BudgetTrackerApplication).repository
                val allCollections = repository.getAllCollectionsSync()
                val selectedNames = CollectionWidgetConfigureActivity.loadSelectedCollections(context, appWidgetId)
                collections = allCollections.filter { it.name in selectedNames }
            }
        }
    }

    override fun onDestroy() {
        // Nothing to do
    }

    override fun getCount(): Int = collections.size

    override fun getViewAt(position: Int): RemoteViews {
        val collection = collections[position]
        val views = RemoteViews(context.packageName, R.layout.widget_collection_item)
        views.setTextViewText(R.id.widget_collection_name, collection.name)

        val fillInIntent = Intent().apply {
            data = Uri.parse("snapspend://collection/${collection.name}")
            putExtra(WidgetEntryActivity.EXTRA_COLLECTION_NAME, collection.name)
        }
        views.setOnClickFillInIntent(R.id.widget_collection_name, fillInIntent)

        return views
    }

    override fun getLoadingView(): RemoteViews? = null

    override fun getViewTypeCount(): Int = 1

    override fun getItemId(position: Int): Long = position.toLong()

    override fun hasStableIds(): Boolean = true
}