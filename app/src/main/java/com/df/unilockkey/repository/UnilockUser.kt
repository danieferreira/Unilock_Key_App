package com.df.unilockkey.repository

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class UnilockUser(
    @PrimaryKey
    val userId: Int = 0,
    val username: String = "",
    val name: String? = "",
    val roles: String? = "",
    val phones: ArrayList<Phone>? = ArrayList<Phone>()
)
