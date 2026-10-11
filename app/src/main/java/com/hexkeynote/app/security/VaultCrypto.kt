package com.hexkeynote.app.security

import java.security.GeneralSecurityException
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

data class NewVault(
    val salt: ByteArray,
    val iterations: Int,
    val keyCheck: ByteArray,
    val key: ByteArray,
)

class InvalidMasterPasswordException : Exception()

object VaultCrypto {
    const val KDF_ITERATIONS = 600_000
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16
    private const val NONCE_BYTES = 12
    private const val TAG_BITS = 128
    private const val MIN_KDF_ITERATIONS = 100_000
    private const val MAX_KDF_ITERATIONS = 2_000_000
    private val keyCheckAad = "HexKeyNote vault key check v1".toByteArray(Charsets.UTF_8)
    private val keyCheckPlaintext = "HexKeyNote key verified".toByteArray(Charsets.UTF_8)
    private val secureRandom = SecureRandom()

    fun createVault(password: CharArray): NewVault {
        require(password.size >= 12) { "마스터 비밀번호는 12자 이상이어야 합니다." }
        val salt = randomBytes(SALT_BYTES)
        val key = deriveKey(password, salt, KDF_ITERATIONS)
        return try {
            NewVault(
                salt = salt,
                iterations = KDF_ITERATIONS,
                keyCheck = encrypt(key, keyCheckAad, keyCheckPlaintext),
                key = key,
            )
        } catch (error: Throwable) {
            key.fill(0)
            throw error
        }
    }

    fun unlock(password: CharArray, salt: ByteArray, iterations: Int, keyCheck: ByteArray): ByteArray {
        val key = deriveKey(password, salt, iterations)
        val verified = try {
            decrypt(key, keyCheckAad, keyCheck)
        } catch (error: GeneralSecurityException) {
            key.fill(0)
            throw InvalidMasterPasswordException()
        } catch (error: IllegalArgumentException) {
            key.fill(0)
            throw InvalidMasterPasswordException()
        }

        val matches = MessageDigest.isEqual(verified, keyCheckPlaintext)
        verified.fill(0)
        if (!matches) {
            key.fill(0)
            throw InvalidMasterPasswordException()
        }
        return key
    }

    fun deriveKey(password: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        require(salt.size == SALT_BYTES) { "잘못된 salt 길이입니다." }
        require(iterations in MIN_KDF_ITERATIONS..MAX_KDF_ITERATIONS) { "지원하지 않는 KDF 반복 횟수입니다." }
        val spec = PBEKeySpec(password, salt, iterations, KEY_BITS)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
                .generateSecret(spec)
                .encoded
        } finally {
            spec.clearPassword()
        }
    }

    fun encrypt(key: ByteArray, associatedData: ByteArray, plaintext: ByteArray): ByteArray {
        val nonce = randomBytes(NONCE_BYTES)
        val ciphertext = encryptWithNonce(key, nonce, associatedData, plaintext)
        return nonce + ciphertext
    }

    fun decrypt(key: ByteArray, associatedData: ByteArray, encrypted: ByteArray): ByteArray {
        require(encrypted.size >= NONCE_BYTES + TAG_BITS / 8) { "암호문이 너무 짧습니다." }
        val nonce = encrypted.copyOfRange(0, NONCE_BYTES)
        val ciphertext = encrypted.copyOfRange(NONCE_BYTES, encrypted.size)
        return decryptWithNonce(key, nonce, associatedData, ciphertext)
    }

    fun encryptWithNonce(
        key: ByteArray,
        nonce: ByteArray,
        associatedData: ByteArray,
        plaintext: ByteArray,
    ): ByteArray {
        require(key.size == KEY_BITS / 8)
        require(nonce.size == NONCE_BYTES)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(associatedData)
        return cipher.doFinal(plaintext)
    }

    fun decryptWithNonce(
        key: ByteArray,
        nonce: ByteArray,
        associatedData: ByteArray,
        ciphertext: ByteArray,
    ): ByteArray {
        require(key.size == KEY_BITS / 8)
        require(nonce.size == NONCE_BYTES)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
        cipher.updateAAD(associatedData)
        return cipher.doFinal(ciphertext)
    }

    fun randomBytes(size: Int): ByteArray = ByteArray(size).also(secureRandom::nextBytes)
}