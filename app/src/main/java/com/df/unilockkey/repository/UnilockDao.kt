package com.df.unilockkey.repository

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query

@Dao
interface UnilockDao {
    @Query("SELECT * FROM unilock")
    fun getAll(): List<Unilock>

    @Query("SELECT * FROM unilock WHERE lockNumber = :lockNumber LIMIT 1")
    fun findByLockNumber(lockNumber: Int): Unilock

    @Insert
    fun insertAll(vararg locks: Unilock)

    @Delete
    fun delete(lock: Unilock)
}