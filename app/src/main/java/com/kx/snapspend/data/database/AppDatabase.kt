package com.kx.snapspend.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.kx.snapspend.data.dao.CollectionsDao
import com.kx.snapspend.data.dao.ExpensesDao
import com.kx.snapspend.model.Collections
import com.kx.snapspend.model.Expenses
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * The Room database for the application.
 * This class ties together the entities, DAOs, and provides the main access point.
 */
@Database(entities = [Expenses::class, Collections::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {

    abstract fun expenseDao(): ExpensesDao
    abstract fun collectionsDao(): CollectionsDao

    companion object {
        // Singleton prevents multiple instances of database opening at the same time.
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            // if the INSTANCE is not null, then return it,
            // if it is, then create the database
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "budget_tracker_database"
                )
                    // Optional: Add a callback to pre-populate the database
                    .addCallback(DatabaseCallback(scope))
                    .build()
                INSTANCE = instance
                // return instance
                instance
            }
        }
    }

    /**
     * A database callback to pre-populate the database with some initial data.
     * This is useful for testing and providing default collections.
     */
    private class DatabaseCallback(
        private val scope: CoroutineScope
    ) : RoomDatabase.Callback() {

        override fun onCreate(db: SupportSQLiteDatabase) {
            super.onCreate(db)
            INSTANCE?.let { database ->
                scope.launch {
                    populateDatabase(database.collectionsDao())
                }
            }
        }

        suspend fun populateDatabase(collectionDao: CollectionsDao) {
            // Add default collections here if you want
            collectionDao.insertCollection(Collections("Single"))
            collectionDao.insertCollection(Collections("Couple"))
            collectionDao.insertCollection(Collections("Family"))
            collectionDao.insertCollection(Collections("Food"))
            collectionDao.insertCollection(Collections("Transport"))
        }
    }
}
