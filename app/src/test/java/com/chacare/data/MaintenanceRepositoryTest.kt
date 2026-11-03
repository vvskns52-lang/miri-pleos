package com.chacare.data

import com.chacare.domain.DwellState
import com.chacare.domain.LifeStatus
import com.chacare.domain.Part
import com.chacare.domain.PendingConfirmation
import com.chacare.domain.Powertrain
import com.chacare.domain.SensorKind
import com.chacare.domain.SignalState
import com.chacare.domain.UsageStats
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.time.LocalDate

class MaintenanceRepositoryTest {
    @get:Rule val tmp = TemporaryFolder()

    private val today = LocalDate.of(2026, 11, 3)
    private val file by lazy { tmp.root.resolve("store.json") }
    private fun newRepo() = MaintenanceRepository(file) { today }
    private val repo by lazy { newRepo() }

    private fun rec(part: Part, type: RecordType, odo: Double) =
        ServiceRecord(part = part, type = type, date = today, odometerKm = odo, source = RecordSource.MANUAL)

    @Test fun refillDoesNotResetLife() = runTest {
        repo.addRecord(rec(Part.ENGINE_OIL, RecordType.REPLACED, odo = 1000.0))
        repo.addRecord(rec(Part.ENGINE_OIL, RecordType.REFILLED, odo = 5000.0))
        assertEquals(1000.0, repo.lastReplacement(Part.ENGINE_OIL)!!.odometerKm, 1e-9)
    }

    @Test fun usageResetsOnReplace() = runTest {
        repo.addUsage(UsageStats(drivenKm = 500.0, brakePresses = 400))
        repo.addRecord(rec(Part.BRAKE_PAD, RecordType.REPLACED, odo = 500.0))
        repo.addUsage(UsageStats(drivenKm = 100.0, brakePresses = 50))
        assertEquals(100.0, repo.usageSince(Part.BRAKE_PAD).drivenKm, 1e-9)
        assertEquals(600.0, repo.usageSince(Part.ENGINE_OIL).drivenKm, 1e-9)
    }

    @Test fun everythingSurvivesRestart() = runTest {
        val dwell = DwellState("s1", 0, 10, false)
        repo.saveDwell(dwell)
        repo.putPending(PendingConfirmation(Part.BRAKE_FLUID, 5))
        repo.saveSensorSnapshot(mapOf(SensorKind.OIL_LEVEL to SignalState.LOW))
        repo.saveOdometer(1234.0)
        repo.addRecord(rec(Part.TIRE, RecordType.REPLACED, odo = 100.0))
        val again = newRepo()
        assertEquals(dwell, again.dwellState())
        assertEquals(Part.BRAKE_FLUID, again.pendings().single().part)
        assertEquals(SignalState.LOW, again.sensorSnapshot()[SensorKind.OIL_LEVEL])
        assertEquals(1234.0, again.lastOdometer()!!, 1e-9)
        assertEquals(today, again.records(Part.TIRE).first().single().date)
    }

    @Test fun pendingAskCountAndRemove() = runTest {
        repo.putPending(PendingConfirmation(Part.ENGINE_OIL, 1))
        repo.bumpAsk(Part.ENGINE_OIL)
        assertEquals(1, repo.pendings().single().askCount)
        repo.removePending(Part.ENGINE_OIL)
        assertTrue(repo.pendings().isEmpty())
    }

    @Test fun editAndDeleteRecord() = runTest {
        repo.addRecord(rec(Part.WIPER_BLADE, RecordType.REPLACED, odo = 10.0))
        val r = repo.records(Part.WIPER_BLADE).first().single()
        repo.updateRecord(r.copy(odometerKm = 20.0))
        assertEquals(20.0, repo.records(Part.WIPER_BLADE).first().single().odometerKm, 1e-9)
        repo.deleteRecord(r.id)
        assertTrue(repo.records(Part.WIPER_BLADE).first().isEmpty())
    }

    @Test fun estimatesSortedUrgentFirst() = runTest {
        repo.setPowertrain(Powertrain.ICE)
        repo.saveOdometer(20_000.0)
        for (p in Part.entries) repo.addRecord(rec(p, RecordType.REPLACED, odo = 20_000.0))
        repo.addRecord(rec(Part.ENGINE_OIL, RecordType.REPLACED, odo = 10_500.0).copy(date = today.minusMonths(1)))
        // 엔진오일 마지막 교체가 위 기록보다 나중(같은 날, 20,000km)이 되지 않도록 앞 기록 삭제
        repo.records(Part.ENGINE_OIL).first().filter { it.odometerKm == 20_000.0 }.forEach { repo.deleteRecord(it.id) }
        val list = repo.estimates().first()
        assertEquals(Part.ENGINE_OIL, list.first().part)
        assertEquals(LifeStatus.REPLACE, list.first().status)
        assertEquals(LifeStatus.GOOD, list.last().status)
    }

    @Test fun evHasSixParts() = runTest {
        repo.setPowertrain(Powertrain.EV)
        assertEquals(6, repo.estimates().first().size)
    }

    @Test fun onboardingFlags() = runTest {
        assertFalse(repo.onboarded())
        assertNull(repo.powertrain())
        repo.setOnboarded()
        assertTrue(newRepo().onboarded())
    }

    @Test fun corruptFileStartsEmpty() = runTest {
        file.writeText("{not json")
        assertTrue(newRepo().pendings().isEmpty())
    }
}
