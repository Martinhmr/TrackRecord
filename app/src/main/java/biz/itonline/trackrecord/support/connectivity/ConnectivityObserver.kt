package biz.itonline.trackrecord.support.connectivity

import kotlinx.coroutines.flow.Flow

fun interface ConnectivityObserver {

    fun observe(): Flow<Boolean>


}