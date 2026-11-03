package com.chacare.location

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/** (위도, 경도) */
interface LocationSource {
    val locations: Flow<Pair<Double, Double>>
    fun start()
    fun stop()
}

class DemoLocationSource : LocationSource {
    override val locations = MutableSharedFlow<Pair<Double, Double>>(replay = 1, extraBufferCapacity = 16)
    override fun start() = Unit
    override fun stop() = Unit
    fun moveTo(lat: Double, lon: Double) { locations.tryEmit(lat to lon) }
}
