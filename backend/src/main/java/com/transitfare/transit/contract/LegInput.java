package com.transitfare.transit.contract;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

/**
 * 요금을 계산할 구간 하나.
 * TRANSIT: operatorCode·lineId(거리제만 필수)·fromStationId·toStationId·trainClass·seatType.
 * WALK: 나머지 값 무시. MANUAL: manualAmount(어른 1명 금액) 필수, operatorCode는 보통 OTHER.
 * continuesPreviousTrain: 앞 구간과 같은 열차를 계속 타고 감(노선만 바뀌는 직통 열차, 예: 특급 유후 久留米 통과).
 * 거리제 운영사에서 지정석·특급요금을 열차 단위로 계산할 때 쓴다. 기본 false — JSON에 없거나 null이면 false로 읽는다
 * (Jackson 3는 빠진 boolean을 실패시키므로 이 필드만 AS_EMPTY로 둔다).
 */
public record LegInput(LegMode mode, String operatorCode, Long lineId,
                       Long fromStationId, Long toStationId,
                       TrainClass trainClass, SeatType seatType,
                       Integer manualAmount,
                       @JsonSetter(nulls = Nulls.AS_EMPTY) boolean continuesPreviousTrain) {
}
