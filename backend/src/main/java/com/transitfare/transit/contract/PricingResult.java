package com.transitfare.transit.contract;

import java.util.List;

/** 구간 요금 목록과 일행 총액. hasUnknown이면 화면에 "미확인 구간 포함" 경고 */
public record PricingResult(List<LegFare> legs, int partyTotal, boolean hasUnknown) {
}
