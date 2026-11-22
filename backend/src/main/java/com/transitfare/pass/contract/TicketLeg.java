package com.transitfare.pass.contract;

import java.time.LocalDate;

/**
 * 승차권 비교용 구간. itinerary가 transit의 LegInput + LegFare를 이 모양으로 바꿔서 넘긴다.
 * fareKnown=false(요금 미확인·직접 입력)인 구간은 승차권으로 커버하지 않는다.
 * groupId: 연속 거리제 묶음(LegFare.groupId 그대로). 묶음 전 구간이 커버될 때만 절감.
 * 금액은 어른 1명 기준: base 운임, expressFare 특급요금, reservedFare 지정석 요금.
 */
public record TicketLeg(LocalDate date, String operatorCode, Long lineId,
                        Long fromStationId, Long toStationId,
                        boolean express, boolean reserved, boolean fareKnown,
                        int base, int expressFare, int reservedFare,
                        Integer groupId) {
}
