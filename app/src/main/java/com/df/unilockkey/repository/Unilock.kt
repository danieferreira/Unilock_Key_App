package com.df.unilockkey.repository

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Unilock  (
    @PrimaryKey
    val lockNumber: Int,
    val keys: ArrayList<Unikey>?,
    val terminalId: String?,
    val branch: Branch?,
    val route: Route?,
    val startDate: String?,
    val endDate: String?,
    var activatedDate: String?,
    var activeKey: Unikey?,
    val duration: Int?,
    var archived: Boolean = false
) {

}

