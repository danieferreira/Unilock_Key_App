package com.df.unilockkey.repository

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface UnilockDao {
    @Query("SELECT * FROM unilock")
    fun getAll(): List<Unilock>

    @Query("SELECT * FROM unilock WHERE lockNumber = :lockNumber LIMIT 1")
    fun findByLockNumber(lockNumber: Int): Unilock

    @Query("SELECT * FROM unilock WHERE archived = :archived")
    fun getAllByArchive(archived: Boolean): List<Unilock>

    @Insert
    fun insertAll(vararg locks: Unilock)

    @Insert
    fun insert(lock: Unilock)

    @Delete
    fun delete(lock: Unilock)

    @Update
    fun update(lock: Unilock)

}