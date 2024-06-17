package com.df.unilockkey.data

sealed interface ConnectionState {
    object Connected: ConnectionState
    object Disconnected: ConnectionState
    object Unitialised: ConnectionState
    object CurrentlyInitialising: ConnectionState

}