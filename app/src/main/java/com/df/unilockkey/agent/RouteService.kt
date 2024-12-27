package com.df.unilockkey.agent

import android.util.Log
import com.df.unilockkey.repository.Route
import com.df.unilockkey.util.ApiEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.net.UnknownHostException

class RouteService(private var api: ApiService) {
    private val coroutineScope= CoroutineScope(Dispatchers.Default)
    val routes: MutableSharedFlow<ApiEvent<Array<Route>>> = MutableSharedFlow()
    val route: MutableSharedFlow<ApiEvent<Route>> = MutableSharedFlow()

    private var busy: Boolean = false

    suspend fun getRoutes() {
        try {
            if (!busy) {
                busy = true;
                val response = api.getRoutes()
                if (response.isSuccessful) {
                    val body = response.body()
                    for (route in body!!) {
                        Log.d("Routes:", "Route: " + route.name)
                    }
                    coroutineScope.launch {
                        routes.emit(
                            ApiEvent.Routes(data = body)
                        )
                    }
                } else {
                    val code = response.code()
                    Log.d("Routes:", code.toString())
                }
            }
        } catch (e: HttpException) {
            val response = e.response()
            val errorCode = e.code()
            if (response != null) {
                Log.d("Routes:", response.message() + ":" + errorCode.toString())
            } else {
                Log.d("Routes:", errorCode.toString())
            }
        } catch (e: UnknownHostException) {
            Log.d("Routes:", e.message.toString())
        } catch (e: Exception) {
            Log.d("Routes:", e.message.toString())
        } finally {
            busy = false;
        }
    }

    suspend fun getRoute(id: Int) {
        try {
            if (!busy) {
                busy = true;
                val response = api.getRoute(id)
                if (response.isSuccessful) {
                    val thisRoute = response.body()
                    if (thisRoute != null) {
                        Log.d("Route:", "Route: " + thisRoute.id)
                        coroutineScope.launch {
                            route.emit(
                                ApiEvent.Route(data = thisRoute)
                            )
                        }
                    }
                } else {
                    val code = response.code()
                    Log.d("Route:", code.toString())
                }
            }
        } catch (e: HttpException) {
            val response = e.response()
            val errorCode = e.code()
            if (response != null) {
                Log.d("Route:", response.message() + ":" + errorCode.toString())
            } else {
                Log.d("Route:", errorCode.toString())
            }
        } catch (e: UnknownHostException) {
            Log.d("Route:", e.message.toString())
        } catch (e: Exception) {
            Log.d("Route:", e.message.toString())
        } finally {
            busy = false
        }
    }
}