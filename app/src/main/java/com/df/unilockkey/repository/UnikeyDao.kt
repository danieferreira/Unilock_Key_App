package com.df.unilockkey.repository

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface UnikeyDao {
    @Query("SELECT * FROM unikey")
    fun getAll(): List<Unikey>

    @Query("SELECT * FROM unikey WHERE keyNumber = :keyNumber LIMIT 1")
    fun findByKeyNumber(keyNumber: Int): Unikey

    @Insert
    fun insert(vararg key: Unikey)

    @Delete
    fun delete(key: Unikey)

    @Update
    fun update(key: Unikey)

}