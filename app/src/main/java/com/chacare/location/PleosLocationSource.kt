package com.chacare.location

import ai.pleos.playground.location.fused.FusedLocation
import ai.pleos.playground.location.fused.listener.FusedLocationListener
import ai.pleos.playground.location.fused.model.LocationData
import android.content.Context
import android.util.Log
import kotlinx.coroutines.flow.MutableSharedFlow

class PleosLocationSource(context: Context) : LocationSource {
    private val fused = FusedLocation(context.applicationContext)

    override val locations = MutableSharedFlow<Pair<Double, Double>>(replay = 1, extraBufferCapacity = 16)

    private val listener = object : FusedLocationListener {
        override fun onChanged(location: LocationData?) {
            location ?: return
            locations.tryEmit(location.latitude to location.longitude)
        }

        override fun onError(exception: Exception) {
            Log.w("ChaCare", "location error: ${exception.message}")
        }
    }

    override fun start() {
        fused.initialize()
        fused.getLastKnownLocation(listener)
        fused.registerFusedLocationCallback(listener)
    }

    override fun stop() {
        runCatching { fused.unregisterFusedLocationCallback(listener) }
        fused.release()
    }
}
