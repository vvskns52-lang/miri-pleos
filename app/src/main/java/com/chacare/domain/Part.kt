package com.chacare.domain

enum class Powertrain { ICE, EV }

enum class Part(val koreanName: String) {
    ENGINE_OIL("엔진오일"),
    BRAKE_FLUID("브레이크액"),
    BRAKE_PAD("브레이크 패드"),
    TRANSMISSION_OIL("변속기 오일"),
    CABIN_FILTER("에어컨 필터"),
    WIPER_BLADE("와이퍼"),
    WASHER_FLUID("워셔액"),
    TIRE("타이어"),
    SPARK_PLUG("점화플러그"),
    ENGINE_AIR_FILTER("에어 필터"),
    COOLANT("냉각수"),
    BATTERY("배터리"),
    DRIVE_BELT("구동벨트"),
}

/** 차량이 직접 알려주는 값(설계서 4.3 ①의 감시 대상). */
enum class SensorKind {
    OIL_LEVEL,
    BRAKE_FLUID_LEVEL,
    TRANSMISSION_OIL_LEVEL,
    WASHER_WARNING,
    TIRE_PRESSURE_WARNING,
}
