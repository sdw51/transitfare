package com.transitfare.transit.contract;

/**
 * 요금을 계산할 구간 하나.
 * TRANSIT: operatorCode·lineId(거리제만 필수)·fromStationId·toStationId·trainClass·seatType.
 * WALK: 나머지 값 무시. MANUAL: manualAmount(어른 1명 금액) 필수, operatorCode는 보통 OTHER.
 */
public record LegInput(LegMode mode, String operatorCode, Long lineId,
                       Long fromStationId, Long toStationId,
                       TrainClass trainClass, SeatType seatType,
                       Integer manualAmount) {
}
