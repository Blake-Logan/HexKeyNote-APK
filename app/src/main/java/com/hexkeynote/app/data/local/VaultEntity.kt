package com.hexkeynote.app.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vault")
data class VaultEntity(
    @PrimaryKey val id: Int = 1,
    val salt: ByteArray,
    val iterations: Int,
    val keyCheck: ByteArray,
)
