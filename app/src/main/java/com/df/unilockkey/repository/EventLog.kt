package com.df.unilockkey.repository

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class EventLog(
    @PrimaryKey(autoGenerate = true)
    var id: Int = 0,
    var timestamp: Long = 0,
    var keyNumber: Int = 0,
    var lockNumber: Int = 0,
    var event: String = "",
    var archived: Boolean = false,
    val battery: String= "",
  )
