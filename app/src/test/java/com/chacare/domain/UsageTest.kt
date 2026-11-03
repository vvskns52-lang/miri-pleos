package com.chacare.domain

import org.junit.Assert.assertEquals
import org.junit.Test

class UsageTest {
    private fun u(
        km: Double = 1000.0, min: Double = 1000.0, brake: Int = 1000, short: Int = 0, trips: Int = 10,
        wiper: Double = 0.0, hvac: Double = 0.0,
    ) = UsageStats(km, min, brake, short, trips, wiper, hvac)

    @Test fun cityBrakingShortensPad() = assertEquals(1.5, correctionFactor(Part.BRAKE_PAD, u(brake = 3000)), 1e-9)
    @Test fun highwayBrakingClampsLow() = assertEquals(0.8, correctionFactor(Part.BRAKE_PAD, u(brake = 100)), 1e-9)
    @Test fun allShortTripsOil() = assertEquals(1.5, correctionFactor(Part.ENGINE_OIL, u(short = 10, trips = 10)), 1e-9)
    @Test fun hvacHalf() = assertEquals(1.15, correctionFactor(Part.CABIN_FILTER, u(hvac = 500.0)), 1e-9)
    @Test fun wiperTwentyPercentIsMax() = assertEquals(1.5, correctionFactor(Part.WIPER_BLADE, u(wiper = 200.0)), 1e-9)
    @Test fun tooLittleData() = assertEquals(1.0, correctionFactor(Part.BRAKE_PAD, u(km = 50.0, brake = 500)), 1e-9)
    @Test fun zeroTripsNoCrash() = assertEquals(1.0, correctionFactor(Part.ENGINE_OIL, u(trips = 0)), 1e-9)
    @Test fun zeroMinutesNoCrash() = assertEquals(1.0, correctionFactor(Part.CABIN_FILTER, u(min = 0.0, hvac = 10.0)), 1e-9)
    @Test fun brakeFluidNeutral() = assertEquals(1.0, correctionFactor(Part.BRAKE_FLUID, u(brake = 5000)), 1e-9)
}
