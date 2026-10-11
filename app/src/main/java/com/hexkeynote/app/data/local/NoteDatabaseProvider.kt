package com.hexkeynote.app.data.local

import android.content.Context
import androidx.room.Room

object NoteDatabaseProvider {
    @Volatile
    private var instance: NoteDatabase? = null

    fun get(context: Context): NoteDatabase = instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(
            context.applicationContext,
            NoteDatabase::class.java,
            "hexkeynote.db",
        ).build().also { instance = it }
    }
}
