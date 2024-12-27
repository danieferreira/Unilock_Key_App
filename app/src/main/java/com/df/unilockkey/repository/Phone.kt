package com.df.unilockkey.repository

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Phone(
    @PrimaryKey
    val id: String,
    val number: String?,
    val description: String?,
    val active: Boolean,
    val user: UnilockUser,
    val routes: ArrayList<Route>
)
