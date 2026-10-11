package com.hexkeynote.app.security

import com.hexkeynote.app.domain.model.Note
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.DataInputStream
import java.io.DataOutputStream
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction

object NotePayloadCodec {
    private const val VERSION = 1
    private const val MAX_STRING_BYTES = 1_048_576
    private const val MAX_NOTES = 10_000

    fun encodeNote(note: Note): ByteArray = ByteArrayOutputStream().use { bytes ->
        DataOutputStream(bytes).use { output ->
            output.writeInt(VERSION)
            writeString(output, note.title)
            writeString(output, note.content)
        }
        bytes.toByteArray()
    }

    fun decodeNote(id: String, createdAt: Long, updatedAt: Long, payload: ByteArray): Note =
        DataInputStream(ByteArrayInputStream(payload)).use { input ->
            require(input.readInt() == VERSION) { "지원하지 않는 노트 형식입니다." }
            val note = Note(
                id = id,
                title = readString(input),
                content = readString(input),
                createdAtMillis = createdAt,
                updatedAtMillis = updatedAt,
            )
            require(input.available() == 0) { "잘못된 노트 데이터입니다." }
            note
        }

    fun encodeBackupNotes(notes: List<Note>): ByteArray {
        require(notes.size <= MAX_NOTES) { "노트가 너무 많습니다." }
        return ByteArrayOutputStream().use { bytes ->
            DataOutputStream(bytes).use { output ->
                output.writeInt(VERSION)
                output.writeInt(notes.size)
                notes.forEach { note ->
                    writeString(output, note.id)
                    writeString(output, note.title)
                    writeString(output, note.content)
                    output.writeLong(note.createdAtMillis)
                    output.writeLong(note.updatedAtMillis)
                }
            }
            bytes.toByteArray()
        }
    }

    fun decodeBackupNotes(payload: ByteArray): List<Note> =
        DataInputStream(ByteArrayInputStream(payload)).use { input ->
            require(input.readInt() == VERSION) { "지원하지 않는 백업 데이터입니다." }
            val count = input.readInt()
            require(count in 0..MAX_NOTES) { "잘못된 노트 개수입니다." }
            val notes = List(count) {
                Note(
                    id = readString(input),
                    title = readString(input),
                    content = readString(input),
                    createdAtMillis = input.readLong(),
                    updatedAtMillis = input.readLong(),
                )
            }
            require(input.available() == 0) { "백업 파일에 알 수 없는 데이터가 있습니다." }
            notes
        }

    private fun writeString(output: DataOutputStream, value: String) {
        val encoded = value.toByteArray(Charsets.UTF_8)
        require(encoded.size <= MAX_STRING_BYTES) { "텍스트가 너무 깁니다." }
        output.writeInt(encoded.size)
        output.write(encoded)
    }

    private fun readString(input: DataInputStream): String {
        val size = input.readInt()
        require(size in 0..MAX_STRING_BYTES && size <= input.available()) { "잘못된 텍스트 길이입니다." }
        val bytes = ByteArray(size)
        input.readFully(bytes)
        return Charsets.UTF_8.newDecoder()
            .onMalformedInput(CodingErrorAction.REPORT)
            .onUnmappableCharacter(CodingErrorAction.REPORT)
            .decode(ByteBuffer.wrap(bytes))
            .toString()
    }
}