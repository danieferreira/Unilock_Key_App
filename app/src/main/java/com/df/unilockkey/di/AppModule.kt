package com.df.unilockkey.di

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import com.df.unilockkey.data.KeyReceiverManager
import com.df.unilockkey.data.ble.KeyBLEReceiverManager
import com.df.unilockkey.repository.DataRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideBluetoothAdaptor(@ApplicationContext context: Context): BluetoothAdapter {
        val manager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        return manager.adapter
    }

    @Provides
    @Singleton
    fun provideKeyReceiverManager(
        @ApplicationContext context: Context,
        bluetoothAdapter: BluetoothAdapter,
        dataRepository: DataRepository
    ): KeyReceiverManager {
        return KeyBLEReceiverManager(bluetoothAdapter, context, dataRepository)
    }

    @Provides
    @Singleton
    fun provideDataRepository(
        @ApplicationContext context: Context
    ): DataRepository {
        return DataRepository(context)
    }
}