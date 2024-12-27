package com.df.unilockkey.repository

import android.content.Context
import androidx.room.Database
import androidx.room.Room.databaseBuilder
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import kotlin.concurrent.Volatile


@Database(entities = [Unikey::class, Unilock::class, EventLog::class, Branch::class, Route::class, Phone::class, UnilockUser::class], version = 12)
@TypeConverters(RoomConverters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun unilockDao(): UnilockDao
    abstract fun unikeyDao(): UnikeyDao
    abstract fun eventLogDao(): EventLogDao
    abstract fun branchDao(): BranchDao
    abstract fun routeDao(): RouteDao
    abstract fun phoneDao(): PhoneDao
    abstract fun unilockUserDao(): UnilockUserDao

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
                )
                    .allowMainThreadQueries()
                    .fallbackToDestructiveMigration()
                    .addTypeConverter(RoomConverters())
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }

}



