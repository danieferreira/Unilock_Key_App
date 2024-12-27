package com.df.unilockkey.repository

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface UnilockUserDao {
    @Query("SELECT * FROM unilockuser")
    fun getAll(): List<UnilockUser>
    @Query("SELECT * FROM unilockuser WHERE userId = :userId LIMIT 1")
    fun findByUserId(userId: Int): UnilockUser
    @Insert
    fun insert(vararg user: UnilockUser)
    @Delete
    fun delete(user: UnilockUser)
    @Update
    fun update(user: UnilockUser)
}