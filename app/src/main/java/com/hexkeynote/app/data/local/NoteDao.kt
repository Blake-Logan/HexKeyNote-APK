package com.hexkeynote.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert

@Dao
interface NoteDao {
    @Query("SELECT * FROM encrypted_notes ORDER BY updatedAtMillis DESC")
    suspend fun getAll(): List<EncryptedNoteEntity>

    @Upsert
    suspend fun upsert(note: EncryptedNoteEntity)

    @Query("DELETE FROM encrypted_notes WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM encrypted_notes")
    suspend fun deleteAll()
}
