package com.kx.snapspend.ui

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.kx.snapspend.BudgetTrackerApplication
import com.kx.snapspend.R
import com.kx.snapspend.widget.CollectionWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CollectionWidgetConfigureActivity : AppCompatActivity() {

    private var appWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
    private lateinit var listView: ListView
    private lateinit var saveButton: Button

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.widget_configure_layout)
        setResult(Activity.RESULT_CANCELED)

        val intent = intent
        val extras = intent.extras
        if (extras != null) {
            appWidgetId = extras.getInt(
                AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID
            )
        }
        if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
            finish()
            return
        }

        listView = findViewById(R.id.configure_list_view)
        saveButton = findViewById(R.id.configure_save_button)

        // ** THIS IS THE CORRECTED PART **
        // We launch a coroutine to do the database work in the background.
        lifecycleScope.launch(Dispatchers.IO) {
            val repository = (application as BudgetTrackerApplication).repository
            val allCollections = repository.getAllCollectionsSync().map { it.name }

            // Switch back to the main thread to update the UI
            withContext(Dispatchers.Main) {
                val adapter = ArrayAdapter(this@CollectionWidgetConfigureActivity, android.R.layout.simple_list_item_multiple_choice, allCollections)
                listView.adapter = adapter
            }
        }

        saveButton.setOnClickListener { onSave() }
    }

    private fun onSave() {
        val selectedCollections = mutableSetOf<String>()
        val checked = listView.checkedItemPositions
        for (i in 0 until listView.adapter.count) {
            if (checked[i]) {
                selectedCollections.add(listView.getItemAtPosition(i) as String)
            }
        }

        saveSelectedCollections(this, appWidgetId, selectedCollections)

        val appWidgetManager = AppWidgetManager.getInstance(this)
        CollectionWidgetProvider.updateAppWidget(this, appWidgetManager, appWidgetId)

        val resultValue = Intent()
        resultValue.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId)
        setResult(Activity.RESULT_OK, resultValue)
        finish()
    }

    companion object {
        private const val PREFS_NAME = "com.kx.snapspend.widget.CollectionWidgetProvider"
        private const val PREF_PREFIX_KEY = "widget_"

        fun saveSelectedCollections(context: Context, appWidgetId: Int, collections: Set<String>) {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0).edit()
            prefs.putStringSet(PREF_PREFIX_KEY + appWidgetId, collections)
            prefs.apply()
        }

        fun loadSelectedCollections(context: Context, appWidgetId: Int): Set<String> {
            val prefs = context.getSharedPreferences(PREFS_NAME, 0)
            return prefs.getStringSet(PREF_PREFIX_KEY + appWidgetId, emptySet()) ?: emptySet()
        }
    }
}