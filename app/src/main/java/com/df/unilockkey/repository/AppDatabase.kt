package com.df.unilockkey.repository

import android.content.Context
import androidx.room.Database
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import kotlin.concurrent.Volatile


@Database(entities = [Unikey::class, Unilock::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun unilockDao(): UnilockDao
    abstract fun unikeyDao(): UnikeyDao

    @Volatile
    private var INSTANCE: AppDatabase? = null
    fun getDatabase(context: Context): AppDatabase? {
        return INSTANCE ?: synchronized(this) {
            INSTANCE?.let {
                return it
            }

            val instance = databaseBuilder(
                context.getApplicationContext(),
                AppDatabase::class.java, "unilock_database"
            )
                .build()
            INSTANCE = instance
            instance
        }
    }

    companion object {
        fun getDatabase(context: Context): AppDatabase {
            return getDatabase(context)
        }
    }

}



