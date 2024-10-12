package com.df.unilockkey.repository

import android.icu.util.Calendar
import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken


class RoomConverters {

    //for date and time convertions
    @TypeConverter
    fun calendarToDateStamp(calendar: Calendar): Long = calendar.timeInMillis

    @TypeConverter
    fun dateStampToCalendar(value: Long): Calendar =
        Calendar.getInstance().apply { timeInMillis = value }

    @TypeConverter
    fun fromUnikey(value: Unikey?): String? {
        return Gson().toJson(value)
    }
    @TypeConverter
    fun toUnikey(value: String?): Unikey {
        return  Gson().fromJson(
            value,
            object : TypeToken<Unikey?>() {}.type
        )
    }

    @TypeConverter
    fun fromUnikeyList(listOfString: ArrayList<Unikey>): String? {
        return Gson().toJson(listOfString)
    }
    @TypeConverter
    fun toUnikeyList(list: String?): ArrayList<Unikey> {
        return Gson().fromJson(
            list,
            object : TypeToken<ArrayList<Unikey?>?>() {}.type
        )
    }

    @TypeConverter
    fun fromUnilock(value: Unilock?): String? {
        return Gson().toJson(value)
    }
    @TypeConverter
    fun toUnilock(value: String?): Unilock {
        return  Gson().fromJson(
            value,
            object : TypeToken<Unilock?>() {}.type
        )
    }

    @TypeConverter
    fun fromUnilockList(listOfString: ArrayList<Unilock>): String? {
        return Gson().toJson(listOfString)
    }
    @TypeConverter
    fun toUnilockList(list: String?): ArrayList<Unilock> {
        return Gson().fromJson(
            list,
            object : TypeToken<ArrayList<Unilock?>?>() {}.type
        )
    }

    @TypeConverter
    fun fromEventLog(value: EventLog?): String? {
        return Gson().toJson(value)
    }
    @TypeConverter
    fun toEventLog(value: String?): EventLog {
        return  Gson().fromJson(
            value,
            object : TypeToken<EventLog?>() {}.type
        )
    }

}