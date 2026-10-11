package com.hexkeynote.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [EncryptedNoteEntity::class, VaultEntity::class],
    version = 1,
    exportSchema = true,
)
abstract class NoteDatabase : RoomDatabase() {
    abstract fun noteDao(): NoteDao
    abstract fun vaultDao(): VaultDao
}
