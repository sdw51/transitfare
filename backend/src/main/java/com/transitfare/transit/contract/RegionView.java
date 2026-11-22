package com.transitfare.transit.contract;

/** 지역 정보. currency는 금액 단위(예: JPY), timezone은 날짜 계산 기준 */
public record RegionView(String code, String nameKo, String currency, String timezone) {
}
