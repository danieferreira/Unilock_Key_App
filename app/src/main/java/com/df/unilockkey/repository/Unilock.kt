package com.df.unilockkey.repository

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Unilock  (
    @PrimaryKey
    val lockNumber: Int = 0,
    val keys: ArrayList<Unikey>? = ArrayList<Unikey>(),
    val terminalId: String? = "",
    val branch: Branch? = Branch(),
    val route: Route? = Route(),
    val startDate: String? = "",
    val endDate: String? = "",
    var activatedDate: String? ="",
    var activeKey: Unikey? = Unikey(),
    val duration: Int? = 0,
    var archived: Boolean = false
) {

}

