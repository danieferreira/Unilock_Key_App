package com.df.unilockkey.data

import java.util.Date

data class KeyInfoResult(
    val keyId: String,
    val lockId: String,
    val battVoltage: Double,
    val lockStatus: Int,
    val keyVersion: String,
    val date: Date?,
    val connectionState: ConnectionState
)
