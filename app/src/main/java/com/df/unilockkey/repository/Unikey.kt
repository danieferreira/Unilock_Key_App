package com.df.unilockkey.repository

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Unikey(
    @PrimaryKey
    val keyNumber: Int,
    val locks: ArrayList<Unilock>,
    val startDate: String?,
    val duration: Int?,
    val enabled: Boolean,
    val timeLimitEnabled: Boolean,
    val startTime: String?,
    val endTime: String?

)
