package com.df.unilockkey.repository

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Unikey(
    @PrimaryKey
    val keyNumber: Int = 0,
    val locks: ArrayList<Unilock>? = ArrayList<Unilock>(),
    val startDate: String? = "",
    val duration: Int? = 0,
    val enabled: Boolean = false,
    val timeLimitEnabled: Boolean = false,
    val startTime: String? = "",
    val endTime: String? = ""
)
