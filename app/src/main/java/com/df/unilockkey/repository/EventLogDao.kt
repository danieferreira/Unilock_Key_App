package com.df.unilockkey.repository

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface EventLogDao {
    @Query("SELECT * FROM eventlog")
    fun getAll(): List<EventLog>

    @Query("SELECT * FROM eventlog WHERE archived = :archived")
    fun getAllByArchive(archived: Boolean): List<EventLog>

    @Query("SELECT * FROM eventlog WHERE id = :eventLogId LIMIT 1")
    fun findById(eventLogId: Int): EventLog

    @Insert
    fun insertAll(vararg events: EventLog)

    @Delete
    fun delete(event: EventLog)

    @Update
    fun update(event: EventLog)

}