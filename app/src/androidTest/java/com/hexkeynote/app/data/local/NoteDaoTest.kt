package com.hexkeynote.app.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NoteDaoTest {
    private lateinit var database: NoteDatabase
    private lateinit var noteDao: NoteDao

    @Before
    fun createDatabase() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, NoteDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        noteDao = database.noteDao()
    }

    @After
    fun closeDatabase() {
        database.close()
    }

    @Test
    fun upsertStoresEncryptedPayloadAndDeleteRemovesRecord() = runBlocking {
        val payload = byteArrayOf(12, 34, 56)
        val note = EncryptedNoteEntity(
            id = "note-1",
            encryptedPayload = payload,
            createdAtMillis = 1L,
            updatedAtMillis = 2L,
        )

        noteDao.upsert(note)

        val stored = noteDao.getAll().single()
        assertEquals(note.id, stored.id)
        assertArrayEquals(payload, stored.encryptedPayload)

        noteDao.deleteById(note.id)

        assertEquals(emptyList<EncryptedNoteEntity>(), noteDao.getAll())
    }
}
