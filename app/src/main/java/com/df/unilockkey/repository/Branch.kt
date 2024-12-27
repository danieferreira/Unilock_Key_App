package com.df.unilockkey.repository

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Branch (
    @PrimaryKey
    val id: Int,
    val name: String,
    val routes: ArrayList<Route>
)