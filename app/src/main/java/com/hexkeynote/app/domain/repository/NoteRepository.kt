package com.hexkeynote.app.domain.repository

import com.hexkeynote.app.domain.model.Note

interface NoteRepository {
    suspend fun getAll(): List<Note>
    suspend fun save(note: Note)
    suspend fun delete(id: String)
}
