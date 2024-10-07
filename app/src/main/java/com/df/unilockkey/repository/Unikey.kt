package com.df.unilockkey.repository

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Unikey(
    @PrimaryKey
    val keyNumber: Int,
    val locks: ArrayList<Unilock>
)
