package com.df.unilockkey.repository

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Route (
    @PrimaryKey
    val id: Int,
    val name: String,
    val locks: ArrayList<Unilock>,
    val branch: Branch
)