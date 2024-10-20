package com.df.unilockkey.util

sealed class  ApiEvent <out T:Any> {
    data class LoggedIn<out T:Any> (val data: T? = null, val message: String):ApiEvent<T>()
    data class Keys<out T:Any> (val data: T):ApiEvent<T>()
    data class Locks<out T:Any> (val data: T):ApiEvent<T>()
    data class Error(val message: String) : ApiEvent<String>()
}