package com.df.unilockkey.repository

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface SettingsDao {
    @Query("SELECT * FROM settings")
    fun getAll(): List<Settings>
    @Query("SELECT * FROM settings WHERE settings.lock = :id AND settings.configured = 0 LIMIT 1")
    fun getAllByLock(id: Long): Settings
    @Query("SELECT * FROM settings WHERE settings.`key` = :id LIMIT 1")
    fun getAllByKey(id: Long): Settings
    @Query("SELECT * FROM settings WHERE id = :id LIMIT 1")
    fun findById(id: Int): Settings
    @Insert
    fun insert(vararg setting: Settings)
    @Delete
    fun delete(setting: Settings)
    @Update
    fun update(setting: Settings)
    @Query("SELECT * FROM settings WHERE settings.archived = :archived AND settings.configured = 1")
    fun getAllByArchive(archived: Boolean): List<Settings>
}