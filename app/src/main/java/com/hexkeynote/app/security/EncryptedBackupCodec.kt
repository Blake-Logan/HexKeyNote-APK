package com.hexkeynote.app.security

import com.hexkeynote.app.domain.model.Note
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer

object EncryptedBackupCodec {
    private val magic = byteArrayOf(0x48, 0x4b, 0x4e, 0x42)
    private const val VERSION = 1
    private const val SALT_BYTES = 16
    private const val NONCE_BYTES = 12
    private const val HEADER_BYTES = 4 + 4 + 4 + SALT_BYTES + NONCE_BYTES
    const val MAX_BACKUP_BYTES = 52_428_800

    fun encode(notes: List<Note>, password: CharArray): ByteArray {
        val plaintext = NotePayloadCodec.encodeBackupNotes(notes)
        require(plaintext.size <= MAX_BACKUP_BYTES) { "백업 파일이 너무 큽니다." }
        val salt = VaultCrypto.randomBytes(SALT_BYTES)
        val nonce = VaultCrypto.randomBytes(NONCE_BYTES)
        val header = ByteArrayOutputStream(HEADER_BYTES).use { bytes ->
            DataOutputStream(bytes).use { output ->
                output.write(magic)
                output.writeInt(VERSION)
                output.writeInt(VaultCrypto.KDF_ITERATIONS)
                output.write(salt)
                output.write(nonce)
            }
            bytes.toByteArray()
        }
        val key = VaultCrypto.deriveKey(password, salt, VaultCrypto.KDF_ITERATIONS)
        return try {
            header + VaultCrypto.encryptWithNonce(key, nonce, header, plaintext)
        } finally {
            key.fill(0)
            plaintext.fill(0)
        }
    }

    fun decode(backup: ByteArray, password: CharArray): List<Note> {
        require(backup.size in (HEADER_BYTES + 16)..MAX_BACKUP_BYTES) { "백업 파일 크기가 올바르지 않습니다." }
        val header = backup.copyOfRange(0, HEADER_BYTES)
        val buffer = ByteBuffer.wrap(header)
        val actualMagic = ByteArray(magic.size).also(buffer::get)
        require(actualMagic.contentEquals(magic)) { "HexKeyNote 백업 파일이 아닙니다." }
        require(buffer.int == VERSION) { "지원하지 않는 백업 버전입니다." }
        val iterations = buffer.int
        require(iterations == VaultCrypto.KDF_ITERATIONS) { "지원하지 않는 백업 KDF 설정입니다." }
        val salt = ByteArray(SALT_BYTES).also(buffer::get)
        val nonce = ByteArray(NONCE_BYTES).also(buffer::get)
        val key = VaultCrypto.deriveKey(password, salt, iterations)
        val ciphertext = backup.copyOfRange(HEADER_BYTES, backup.size)
        val plaintext = try {
            VaultCrypto.decryptWithNonce(key, nonce, header, ciphertext)
        } finally {
            key.fill(0)
        }
        return try {
            NotePayloadCodec.decodeBackupNotes(plaintext)
        } finally {
            plaintext.fill(0)
        }
    }
}