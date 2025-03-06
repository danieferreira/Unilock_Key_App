package com.df.unilockkey.repository

data class DebugLog(
    var phoneId: String? = "",
    var timestamp: Long = 0,
    var event: String = "",
  )

