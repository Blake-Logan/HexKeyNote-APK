package com.hexkeynote.app.data.repository

import androidx.room.withTransaction
import com.hexkeynote.app.data.local.EncryptedNoteEntity
import com.hexkeynote.app.data.local.NoteDatabase
import com.hexkeynote.app.data.local.VaultEntity
import com.hexkeynote.app.domain.model.Note
import com.hexkeynote.app.security.NotePayloadCodec
import com.hexkeynote.app.security.VaultCrypto
import java.security.MessageDigest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class EncryptedNoteRepository(private val database: NoteDatabase) {
    suspend fun hasVault(): Boolean = withContext(Dispatchers.IO) {
        database.vaultDao().get() != null
    }

    suspend fun createVault(password: CharArray): ByteArray = withContext(Dispatchers.IO) {
        val vault = VaultCrypto.createVault(password)
        try {
            database.withTransaction {
                check(database.vaultDao().get() == null) { "금고가 이미 생성되어 있습니다." }
                database.vaultDao().insert(
                    VaultEntity(
                        salt = vault.salt,
                        iterations = vault.iterations,
                        keyCheck = vault.keyCheck,
                    ),
                )
            }
            vault.key
        } catch (error: Throwable) {
            vault.key.fill(0)
            throw error
        }
    }

    suspend fun unlock(password: CharArray): ByteArray = withContext(Dispatchers.IO) {
        val vault = requireNotNull(database.vaultDao().get()) { "설정된 금고가 없습니다." }
        VaultCrypto.unlock(password, vault.salt, vault.iterations, vault.keyCheck)
    }

    suspend fun verifyPassword(password: CharArray, activeKey: ByteArray): Boolean =
        withContext(Dispatchers.IO) {
            val candidate = unlock(password)
            try {
                MessageDigest.isEqual(candidate, activeKey)
            } finally {
                candidate.fill(0)
            }
        }

    suspend fun getAll(key: ByteArray): List<Note> = withContext(Dispatchers.IO) {
        database.noteDao().getAll().map { entity ->
            val plaintext = VaultCrypto.decrypt(key, entity.id.toByteArray(Charsets.UTF_8), entity.encryptedPayload)
            try {
                NotePayloadCodec.decodeNote(
                    id = entity.id,
                    createdAt = entity.createdAtMillis,
                    updatedAt = entity.updatedAtMillis,
                    payload = plaintext,
                )
            } finally {
                plaintext.fill(0)
            }
        }
    }

    suspend fun save(key: ByteArray, note: Note) = withContext(Dispatchers.IO) {
        val plaintext = NotePayloadCodec.encodeNote(note)
        val encrypted = try {
            VaultCrypto.encrypt(key, note.id.toByteArray(Charsets.UTF_8), plaintext)
        } finally {
            plaintext.fill(0)
        }
        database.noteDao().upsert(
            EncryptedNoteEntity(
                id = note.id,
                encryptedPayload = encrypted,
                createdAtMillis = note.createdAtMillis,
                updatedAtMillis = note.updatedAtMillis,
            ),
        )
    }

    suspend fun delete(id: String) = withContext(Dispatchers.IO) {
        database.noteDao().deleteById(id)
    }

    suspend fun replaceAll(key: ByteArray, notes: List<Note>) = withContext(Dispatchers.IO) {
        val encryptedNotes = notes.map { note ->
            val plaintext = NotePayloadCodec.encodeNote(note)
            val encrypted = try {
                VaultCrypto.encrypt(key, note.id.toByteArray(Charsets.UTF_8), plaintext)
            } finally {
                plaintext.fill(0)
            }
            EncryptedNoteEntity(note.id, encrypted, note.createdAtMillis, note.updatedAtMillis)
        }
        database.withTransaction {
            database.noteDao().deleteAll()
            encryptedNotes.forEach { note ->
                database.noteDao().upsert(note)
            }
        }
    }
}