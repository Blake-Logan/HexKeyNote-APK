package com.hexkeynote.app.security

import com.hexkeynote.app.domain.model.Note
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class VaultCryptoTest {
    @Test
    fun createdVaultUnlocksWithCorrectPassword() {
        val password = "correct horse battery staple".toCharArray()
        val vault = VaultCrypto.createVault(password)
        val unlocked = VaultCrypto.unlock(password, vault.salt, vault.iterations, vault.keyCheck)

        try {
            assertEquals(true, vault.key.contentEquals(unlocked))
        } finally {
            password.fill(' ')
            vault.key.fill(0)
            unlocked.fill(0)
        }
    }

    @Test
    fun wrongPasswordIsRejected() {
        val password = "correct horse battery staple".toCharArray()
        val vault = VaultCrypto.createVault(password)
        val wrongPassword = "wrong password phrase".toCharArray()

        try {
            assertThrows(InvalidMasterPasswordException::class.java) {
                VaultCrypto.unlock(wrongPassword, vault.salt, vault.iterations, vault.keyCheck)
            }
        } finally {
            password.fill(' ')
            wrongPassword.fill(' ')
            vault.key.fill(0)
        }
    }

    @Test
    fun noteCipherRejectsTamperedCiphertext() {
        val key = VaultCrypto.randomBytes(32)
        val aad = "note-id".toByteArray()
        val ciphertext = VaultCrypto.encrypt(key, aad, "private note".toByteArray())
        ciphertext[ciphertext.lastIndex] = (ciphertext.last().toInt() xor 1).toByte()

        try {
            assertThrows(javax.crypto.AEADBadTagException::class.java) {
                VaultCrypto.decrypt(key, aad, ciphertext)
            }
        } finally {
            key.fill(0)
        }
    }

    @Test
    fun encryptedBackupRoundTripsNotes() {
        val password = "correct horse battery staple".toCharArray()
        val notes = listOf(Note("id-1", "Title", "Body", 10L, 20L))
        val backup = EncryptedBackupCodec.encode(notes, password)

        try {
            assertEquals(notes, EncryptedBackupCodec.decode(backup, password))
            assertThrows(javax.crypto.AEADBadTagException::class.java) {
                EncryptedBackupCodec.decode(backup, "another password phrase".toCharArray())
            }
        } finally {
            password.fill(' ')
            backup.fill(0)
        }
    }
}