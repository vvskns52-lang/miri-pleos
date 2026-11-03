package com.chacare.location

import com.chacare.domain.Shop

/** 시연용으로 미리 등록한 정비소(설계서 5.6). 지도 API 연동은 하지 않는다. */
val DEMO_SHOPS: List<Shop> = listOf(
    Shop("demo-1", "시청 정비센터", 37.5665, 126.9780),
    Shop("demo-2", "양재 정비센터", 37.4840, 127.0347),
    Shop("demo-3", "판교 정비센터", 37.3947, 127.1112),
)

/** 데모 모드에서 차가 처음 서 있는 곳(정비소와 1km 이상 떨어진 곳). */
const val DEMO_HOME_LAT = 37.5759
const val DEMO_HOME_LON = 126.9768
