package com.safephrase.location

import android.annotation.SuppressLint
import android.content.Context
import android.os.Looper
import com.google.android.gms.location.*
import com.safephrase.core.util.SafeLogger
import com.safephrase.domain.model.LocationResult
import com.safephrase.domain.model.LocationSource
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "LocationProvider"

/**
 * Provides best-available location with configurable timeout.
 * Strategy:
 *  1. Try fresh location via FusedLocation with timeout.
 *  2. Fall back to lastKnownLocation.
 *  3. Return null with UNAVAILABLE source if both fail.
 */
@Singleton
class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fused = LocationServices.getFusedLocationProviderClient(context)

    @SuppressLint("MissingPermission")
    suspend fun getCurrentOrLastKnown(timeoutMs: Long): LocationResult? {
        return try {
            withTimeoutOrNull(timeoutMs) {
                val result = fused.getCurrentLocation(
                    Priority.PRIORITY_HIGH_ACCURACY,
                    null
                ).await()
                result?.let { loc ->
                    LocationResult(
                        latitude = loc.latitude,
                        longitude = loc.longitude,
                        accuracyMeters = loc.accuracy,
                        timestamp = loc.time,
                        source = LocationSource.GPS
                    )
                }
            } ?: getLastKnown()
        } catch (e: SecurityException) {
            SafeLogger.w(TAG, "Location permission denied")
            null
        } catch (e: Exception) {
            SafeLogger.e(TAG, "Location error", e)
            getLastKnown()
        }
    }

    @SuppressLint("MissingPermission")
    private suspend fun getLastKnown(): LocationResult? {
        return try {
            val loc = fused.lastLocation.await()
            loc?.let {
                val ageMs = System.currentTimeMillis() - it.time
                SafeLogger.d(TAG, "Using last-known location (age=${ageMs}ms)")
                LocationResult(
                    latitude = it.latitude,
                    longitude = it.longitude,
                    accuracyMeters = it.accuracy,
                    timestamp = it.time,
                    source = LocationSource.LAST_KNOWN
                )
            }
        } catch (e: Exception) {
            SafeLogger.w(TAG, "Last known location unavailable")
            null
        }
    }
}
