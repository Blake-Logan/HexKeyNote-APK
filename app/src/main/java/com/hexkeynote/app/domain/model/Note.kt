package com.hexkeynote.app.domain.model

data class Note(
    val id: String,
    val title: String,
    val content: String,
    val createdAtMillis: Long,
    val updatedAtMillis: Long,
)
