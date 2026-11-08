package com.chacare.domain

// "곧"의 기준은 앱이 정한 값(설계서 5.15).
private const val SOON_KM = 3_000.0
private const val SOON_MONTHS = 2.0

/** 교체(점검) 시기가 됐거나 곧 되는 항목. 이력을 모르거나 주기가 없는 항목은 뺀다. */
fun isDueSoon(e: LifeEstimate): Boolean {
    if (e.kind == IntervalKind.NONE || e.remainingRatio == null) return false
    return e.status == LifeStatus.REPLACE ||
        (e.remainingKm != null && e.remainingKm <= SOON_KM) ||
        (e.remainingMonths != null && e.remainingMonths <= SOON_MONTHS)
}

/** 정비소 한 번에 함께 처리하면 좋은 묶음(급한 순). 두 개 이상일 때만 묶음이다. */
fun bundleOf(estimates: List<LifeEstimate>): List<LifeEstimate> {
    val due = estimates.filter(::isDueSoon).sortedWith(
        compareBy<LifeEstimate>({ it.status != LifeStatus.REPLACE }, { it.status != LifeStatus.CHECK }, { it.remainingRatio }),
    )
    return if (due.size >= 2) due else emptyList()
}
