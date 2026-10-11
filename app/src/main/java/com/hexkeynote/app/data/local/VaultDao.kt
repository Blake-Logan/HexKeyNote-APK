package com.hexkeynote.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface VaultDao {
    @Query("SELECT * FROM vault WHERE id = 1 LIMIT 1")
    suspend fun get(): VaultEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(vault: VaultEntity)
}
