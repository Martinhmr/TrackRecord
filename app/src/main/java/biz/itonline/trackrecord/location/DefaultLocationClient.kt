package biz.itonline.trackrecord.location

import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import biz.itonline.trackrecord.support.HZConstants
import biz.itonline.trackrecord.support.hasLocationPermission
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import kotlin.collections.lastOrNull
import kotlin.collections.mapIndexed
import kotlin.collections.sum

class DefaultLocationClient(
    private val context: Context
): LocationClient {
private val client = LocationServices.getFusedLocationProviderClient(context)

    private val locationBuffer = mutableListOf<Location>()
    private val bufferSize = 20

    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(interval: Long): Flow<Location> {
        return callbackFlow {
            if (!context.hasLocationPermission()) {
                throw LocationClient.LocationException("Missing location permission")
            }

            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

            val isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)

            val isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!isGpsEnabled && !isNetworkEnabled) {
                throw LocationClient.LocationException("GPS is disabled")
            }

            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval).apply {
                setWaitForAccurateLocation(true)
                setMinUpdateIntervalMillis(interval / 4)
            }.build()

            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    super.onLocationResult(result)
                    result.locations.lastOrNull()?.let { location ->
                        if (location.accuracy <= HZConstants.LOCATION_ACCURACY_MAX) {
                            locationBuffer.add(location)

                            if (locationBuffer.size > bufferSize) {
                                locationBuffer.removeAt(0)
                            }

                            val weights = List(locationBuffer.size) { it + 1 }
                            val sumOfWeights = weights.sum()
                            val averageLatitude =
                                locationBuffer.mapIndexed { index, loc -> weights[index] * loc.latitude }
                                    .sum() / sumOfWeights
                            val averageLongitude =
                                locationBuffer.mapIndexed { index, loc -> weights[index] * loc.longitude }
                                    .sum() / sumOfWeights
                            val averageAltitude =
                                locationBuffer.mapIndexed { index, loc -> weights[index] * loc.altitude }
                                    .sum() / sumOfWeights

                            val averageLocation = Location(location.provider)
                            averageLocation.latitude = averageLatitude
                            averageLocation.longitude = averageLongitude
                            averageLocation.altitude = averageAltitude
                            averageLocation.accuracy = location.accuracy
                            averageLocation.bearing = location.bearing
                            averageLocation.speed = location.speed
                            averageLocation.time = location.time
                            averageLocation.elapsedRealtimeNanos = location.elapsedRealtimeNanos
                            averageLocation.provider = location.provider
                            averageLocation.extras = location.extras

                            launch { send(averageLocation) }
                        }
                    }
                }
            }

            client.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            )

            awaitClose {
                client.removeLocationUpdates(locationCallback)
            }
        }
    }
}