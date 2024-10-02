package com.df.unilockkey.repository

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Unilock  (
    @PrimaryKey
    val lockNumber: Int,
    val keys: List<Unikey>
    )