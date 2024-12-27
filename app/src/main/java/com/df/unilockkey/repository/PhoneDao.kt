package com.df.unilockkey.repository

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface PhoneDao {
    @Query("SELECT * FROM phone")
    fun getAll(): List<Phone>
    @Query("SELECT * FROM phone WHERE id = :id LIMIT 1")
    fun findById(id: String): Phone
    @Insert
    fun insert(vararg phone: Phone)
    @Delete
    fun delete(phone: Phone)
    @Update
    fun update(phone: Phone)
}