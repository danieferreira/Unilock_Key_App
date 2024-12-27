package com.df.unilockkey.repository

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update

@Dao
interface RouteDao {
    @Query("SELECT * FROM route")
    fun getAll(): List<Route>
    @Query("SELECT * FROM route WHERE id = :id LIMIT 1")
    fun findById(id: Int): Route
    @Insert
    fun insert(vararg route: Route)
    @Delete
    fun delete(route: Route)
    @Update
    fun update(route: Route)
}