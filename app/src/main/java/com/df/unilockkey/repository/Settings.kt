package com.df.unilockkey.repository

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Settings(
    @PrimaryKey
    val id: Int,
    val lock: Unilock?,
    val key: Unikey?,
    var password1: String,
    val password2: String,
    val password3: String,
    val oldPropgrammingPassword: String?,
    val newPropgrammingPassword: String?,
    var configured: Boolean = false,
    val created: Long = 0,
    var implemented: Long = 0,
    var archived: Boolean = false,
)
