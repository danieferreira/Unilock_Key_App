package com.df.unilockkey.repository

import android.content.Context
import androidx.room.Database
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import kotlin.concurrent.Volatile


@Database(entities = [Unikey::class, Unilock::class], version = 1)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun unilockDao(): UnilockDao
    abstract fun unikeyDao(): UnikeyDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE?.let {
                    return it
                }

                val instance = databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java, "unilock_database"
                ).allowMainThreadQueries().build()
                INSTANCE = instance
                instance
            }
        }
    }

}



