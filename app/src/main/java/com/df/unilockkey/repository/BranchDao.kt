package com.df.unilockkey.repository

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface BranchDao {
    @Query("SELECT * FROM branch")
    fun getAll(): List<Branch>
    @Query("SELECT * FROM branch WHERE id = :id LIMIT 1")
    fun findById(id: Int): Branch
    @Insert
    fun insert(vararg branch: Branch)
    @Delete
    fun delete(branch: Branch)
    @Update
    fun update(branch: Branch)
}