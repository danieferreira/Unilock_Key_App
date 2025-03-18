package com.df.unilockkey.service

import com.df.unilockkey.repository.AppDatabase
import com.df.unilockkey.repository.Settings
import javax.inject.Inject

class SettingsService @Inject constructor(
private val appDatabase: AppDatabase,
){
    lateinit var currentSetting: Settings

    fun findKeySetting(keyNumber: Long): Settings? {
        val settings = appDatabase.settingsDao().getAllByArchive(false, false)
        for (setting in settings) {
            if ((setting.key != null) && (setting.key.keyNumber.toLong() == keyNumber) && (setting.configured == false)) {
                currentSetting = setting;
                return setting
            }
        }
        return null;

        //return appDatabase.settingsDao().getAllByKey(keyNumber)
    }

    fun findLockSetting(lockNumber: Long): Settings? {
        val settings = appDatabase.settingsDao().getAllByArchive(false, false)
        for (setting in settings) {
            if ((setting.lock != null) && (setting.lock.lockNumber.toLong() == lockNumber) && (setting.configured == false)) {
                currentSetting = setting;
                return setting
            }
        }
        return null;

        //return appDatabase.settingsDao().getAllByKey(keyNumber)
    }


    fun update(setting: Settings) {
        appDatabase.settingsDao().update(setting)
    }
}