package com.chacare.engine

import com.chacare.data.MaintenanceRepository
import com.chacare.data.RecordSource
import com.chacare.data.RecordType
import com.chacare.data.ServiceRecord
import com.chacare.domain.Part
import com.chacare.domain.SensorKind
import com.chacare.domain.Shop
import com.chacare.domain.SignalState
import com.chacare.domain.UsageStats
import com.chacare.location.DemoLocationSource
import com.chacare.vehicle.DemoVehicleSignals
import com.chacare.voice.FakeVoiceIO
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class MaintenanceEngineTest {
    @get:Rule val tmp = TemporaryFolder()

    private val shop = Shop("s1", "테스트정비", 37.5, 127.0)
    private val nearLat = 37.5003
    private val min = 60_000L
    private val repo by lazy { MaintenanceRepository(tmp.root.resolve("store.json")) { LocalDate.of(2026, 11, 3) } }
    private val vehicle = DemoVehicleSignals(parked = false)
    private val location = DemoLocationSource()

    private var engineError: Throwable? = null

    // backgroundScope 작업은 advanceUntilIdle()이 돌리지 않으므로 같은 스케줄러의 별도 스코프에서 띄운다.
    private fun TestScope.engine(voice: FakeVoiceIO) =
        MaintenanceEngine(vehicle, location, voice, repo, listOf(shop)) { testScheduler.currentTime }
            .also {
                val onError = CoroutineExceptionHandler { _, e -> engineError = e }
                it.start(CoroutineScope(StandardTestDispatcher(testScheduler) + SupervisorJob() + onError))
                runCurrent()
            }

    @After fun noEngineCrash() = assertNull(engineError)

    private fun TestScope.brakeFluidRecovered() {
        vehicle.emitSensor(SensorKind.BRAKE_FLUID_LEVEL, SignalState.LOW)
        vehicle.emitSensor(SensorKind.BRAKE_FLUID_LEVEL, SignalState.NORMAL)
        advanceUntilIdle()
    }

    @Test fun sensorFlowRecordsReplacement() = runTest {
        val voice = FakeVoiceIO(listOf("응 교체했어"))
        engine(voice)
        brakeFluidRecovered()
        vehicle.setParked(true); advanceUntilIdle()
        val r = repo.records(Part.BRAKE_FLUID).first().single()
        assertEquals(RecordType.REPLACED, r.type)
        assertEquals(RecordSource.SENSOR_VOICE, r.source)
        assertTrue(voice.spoken.first().startsWith("브레이크액 교체하셨나요?"))
        assertTrue(repo.pendings().isEmpty())
    }

    @Test fun refillAnswerKeepsLife() = runTest {
        engine(FakeVoiceIO(listOf("보충만 했어")))
        brakeFluidRecovered()
        vehicle.setParked(true); advanceUntilIdle()
        assertEquals(RecordType.REFILLED, repo.records(Part.BRAKE_FLUID).first().single().type)
    }

    @Test fun shopVisitRecordsMultipleParts() = runTest {
        engine(FakeVoiceIO(listOf("엔진오일이랑 와이퍼")))
        location.moveTo(nearLat, 127.0); advanceTimeBy(31 * min)
        location.moveTo(nearLat, 127.0); advanceUntilIdle()
        vehicle.setParked(true); advanceUntilIdle()
        assertEquals(1, repo.records(Part.ENGINE_OIL).first().size)
        assertEquals(RecordSource.SHOP_VOICE, repo.records(Part.WIPER_BLADE).first().single().source)
        assertEquals(null, repo.pendingShopVisit())
    }

    @Test fun shopAnswerMatchesCustomPart() = runTest {
        val id = repo.addCustomPart("블랙박스 메모리", null, null)
        engine(FakeVoiceIO(listOf("엔진오일이랑 블랙박스 메모리 바꿨어")))
        location.moveTo(nearLat, 127.0); advanceTimeBy(31 * min)
        location.moveTo(nearLat, 127.0); advanceUntilIdle()
        vehicle.setParked(true); advanceUntilIdle()
        assertEquals(1, repo.records(Part.ENGINE_OIL).first().size)
        assertEquals(1, repo.customRecords(id).first().size)
    }

    @Test fun silenceKeepsPending() = runTest {
        engine(FakeVoiceIO(listOf(null)))
        brakeFluidRecovered()
        vehicle.setParked(true); advanceUntilIdle()
        assertEquals(1, repo.pendings().single().askCount)
        assertTrue(repo.records(Part.BRAKE_FLUID).first().isEmpty())
    }

    @Test fun drivingAwayAbortsPrompt() = runTest {
        val voice = FakeVoiceIO(listOf("응")) { vehicle.setParked(false) }
        engine(voice)
        brakeFluidRecovered()
        vehicle.setParked(true); advanceUntilIdle()
        assertTrue(repo.records(Part.BRAKE_FLUID).first().isEmpty())
        assertEquals(0, repo.pendings().single().askCount)
        assertTrue(voice.stopped)
    }

    @Test fun onlyOncePerCycle() = runTest {
        val voice = FakeVoiceIO(listOf(null, null))
        val e = engine(voice)
        brakeFluidRecovered()
        vehicle.setParked(true); advanceUntilIdle()
        vehicle.setParked(false); advanceUntilIdle()
        vehicle.setParked(true); advanceUntilIdle()
        assertEquals(1, voice.spoken.count { "교체하셨나요" in it })
        e.newIgnitionCycle(); advanceUntilIdle() // 정차 중 새 시동 → 바로 다시 묻는다
        assertEquals(2, voice.spoken.count { "교체하셨나요" in it })
        vehicle.setParked(false); advanceUntilIdle()
        vehicle.setParked(true); advanceUntilIdle()
        assertEquals(2, voice.spoken.count { "교체하셨나요" in it })
        assertTrue(repo.pendings().isEmpty()) // 두 번 무응답 → 대기 삭제
    }

    @Test fun odometerGlitchIgnored() = runTest {
        engine(FakeVoiceIO(emptyList()))
        for (v in listOf(1000.0, 900.0, null, 1100.0)) { vehicle.setOdometer(v); advanceUntilIdle() }
        assertEquals(100.0, repo.usageSince(Part.BRAKE_PAD).drivenKm, 1e-9)
        assertEquals(1100.0, repo.lastOdometer()!!, 1e-9)
    }

    @Test fun tripsAndBrakesCounted() = runTest {
        engine(FakeVoiceIO(emptyList()))
        vehicle.setOdometer(0.0); advanceUntilIdle()
        vehicle.addKm(5.0); vehicle.pressBrake(3); advanceTimeBy(10 * min)
        vehicle.setParked(true); advanceUntilIdle()
        val u = repo.usageSince(Part.BRAKE_PAD)
        assertEquals(1, u.totalTrips); assertEquals(1, u.shortTrips); assertEquals(3, u.brakePresses)
        assertEquals(10.0, u.drivingMinutes, 0.1)
    }

    @Test fun overdueAlertSpokenOnce() = runTest {
        repo.addRecord(ServiceRecord(part = Part.ENGINE_OIL, type = RecordType.REPLACED, date = LocalDate.of(2026, 11, 1), odometerKm = 0.0, source = RecordSource.MANUAL))
        val voice = FakeVoiceIO(emptyList())
        val e = engine(voice)
        vehicle.setOdometer(0.0); advanceUntilIdle(); vehicle.setOdometer(14_000.0); advanceUntilIdle()
        vehicle.setParked(true); advanceUntilIdle()
        e.newIgnitionCycle(); vehicle.setParked(false); advanceUntilIdle(); vehicle.setParked(true); advanceUntilIdle()
        assertEquals(1, voice.spoken.count { it == "엔진오일 교체 시기가 됐어요." })
    }

    @Test fun severeConditionAnnouncedOnce() = runTest {
        repo.addUsage(UsageStats(drivenKm = 300.0, drivingMinutes = 600.0, shortTrips = 15, totalTrips = 20))
        val voice = FakeVoiceIO(emptyList())
        val e = engine(voice)
        vehicle.setParked(true); advanceUntilIdle()
        e.newIgnitionCycle(); advanceUntilIdle()
        assertEquals(1, voice.spoken.count { "가혹 조건" in it })
        assertTrue(voice.spoken.first { "가혹 조건" in it }.contains("짧은 거리 반복 주행"))
    }

    @Test fun washerRecoveredRecordsRefill() = runTest {
        engine(FakeVoiceIO(emptyList()))
        vehicle.emitSensor(SensorKind.WASHER_WARNING, SignalState.LOW)
        vehicle.emitSensor(SensorKind.WASHER_WARNING, SignalState.NORMAL); advanceUntilIdle()
        assertEquals(RecordType.REFILLED, repo.records(Part.WASHER_FLUID).first().single().type)
    }

    @Test fun restartAtShopCountsVisit() = runTest {
        val first = engine(FakeVoiceIO(emptyList()))
        location.moveTo(nearLat, 127.0); advanceUntilIdle()
        first.stop()
        advanceTimeBy(60 * min)
        engine(FakeVoiceIO(emptyList()))
        location.moveTo(nearLat, 127.0); advanceUntilIdle()
        assertNotNull(repo.pendingShopVisit())
    }
}
