package com.kx.snapspend.data.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.kx.snapspend.model.Collections


@Dao
interface CollectionsDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertCollection(collection: Collections)

    // Gets all collections, ordered alphabetically.
    @Query("SELECT * FROM collections ORDER BY name ASC")
    fun getAllCollections(): LiveData<List<Collections>>

    // Deletes a collection by its name.
    @Query("DELETE FROM collections WHERE name = :collectionName")
    suspend fun deleteCollection(collectionName: String)

    @Query("SELECT * FROM collections ORDER BY name ASC")
    fun getAllCollectionsSync(): List<Collections>
}