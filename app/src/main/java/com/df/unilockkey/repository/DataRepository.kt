package com.df.unilockkey.repository

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow


class DataRepository(context: Context) {
    private val sharedPreferences =
        context.applicationContext.getSharedPreferences("Setting", Context.MODE_PRIVATE)

        private val _keyEnabledFlow = MutableStateFlow(true)
        val keyEnabledFlow: StateFlow<Boolean> = _keyEnabledFlow

        var keyEnabled: Boolean
            get () {
                _keyEnabledFlow.value = sharedPreferences.getBoolean("keyEnabled", true)
                return _keyEnabledFlow.value
            }
            set(value) {
                sharedPreferences.edit().putBoolean("keyEnabled", value).apply()
            }

    companion object {
        @Volatile
        private var INSTANCE: DataRepository? = null

        fun getInstance(context: Context): DataRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE?.let {
                    return it
                }

                val instance = DataRepository(context)
                INSTANCE = instance
                instance
            }
        }
    }
}