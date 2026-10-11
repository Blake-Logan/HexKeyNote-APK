package com.hexkeynote.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "encrypted_notes")
data class EncryptedNoteEntity(
    @PrimaryKey val id: String,
    val encryptedPayload: ByteArray,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)

