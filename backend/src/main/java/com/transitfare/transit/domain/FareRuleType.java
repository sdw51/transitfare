package com.transitfare.transit.domain;

/** 운영사별 요금 계산 방식 */
public enum FareRuleType {
    /** 누적 km + 거리 구간 요금 (지하철, JR) */
    DISTANCE_BAND,
    /** 역·정류장 쌍 요금표 */
    STATION_PAIR,
    /** 균일 구간이면 고정 요금, 아니면 쌍 요금 */
    FLAT_ZONE_PLUS_PAIR,
    /** 직접 입력만 */
    MANUAL_ONLY
}
