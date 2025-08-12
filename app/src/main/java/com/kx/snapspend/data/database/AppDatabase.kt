package com.kx.snapspend.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
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
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // NEW: Define the migration
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // Add the new 'budget' column to the 'collections' table with a default value of 0.0
                db.execSQL("ALTER TABLE collections ADD COLUMN budget REAL NOT NULL DEFAULT 0.0")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE collections ADD COLUMN iconName TEXT NOT NULL DEFAULT 'Label'")
                db.execSQL("ALTER TABLE collections ADD COLUMN colorHex TEXT NOT NULL DEFAULT '#FF6200EE'")
            }
        }
        // NEW: Define the migration from version 3 to 4
        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE collections ADD COLUMN sharePin TEXT")
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE expenses ADD COLUMN cloudId TEXT")
            }
        }

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "budget_tracker_database"
                )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5) // <-- Add new migration
                    .build()
                INSTANCE = instance
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
