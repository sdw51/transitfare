package com.transitfare.pass.domain;

import com.transitfare.pass.contract.ProductType;
import com.transitfare.pass.contract.TicketLeg;
import java.util.List;

/** 최적화에 쓰는 승차권 상품. price: 어른 1명 가격(사용자 설정 가격이 있으면 그 값), uses: FIXED_ROUTE 승차 횟수(PASS는 0 = 무제한). */
public record ProductSpec(String code, ProductType type, int price, int validDays, int uses,
                          boolean coversExpress, int reservedSeatLimit, List<Coverage> coverage) {

    /** 규칙: 적용 범위 중 하나라도 이 구간에 맞으면 커버 대상이다(유효 기간·횟수는 TicketOptimizer가 따로 본다). */
    public boolean covers(TicketLeg leg) {
        return coverage.stream().anyMatch(c -> c.covers(leg));
    }

    /** 적용 범위. lineId가 null이면 운영사 전 노선, 역 두 개가 있으면 구간 양 끝 쌍(방향 무관). */
    public record Coverage(String operatorCode, Long lineId, Long stationAId, Long stationBId) {

        /** 규칙: 운영사가 같고, 노선이 지정돼 있으면 노선도 같고, 역 쌍이 지정돼 있으면 구간 양 끝이 그 쌍(방향 무관). */
        public boolean covers(TicketLeg leg) {
            if (!operatorCode.equals(leg.operatorCode())) {
                return false;
            }
            if (lineId != null && !lineId.equals(leg.lineId())) {
                return false;
            }
            if (stationAId == null || stationBId == null) {
                return true;
            }
            return (stationAId.equals(leg.fromStationId()) && stationBId.equals(leg.toStationId()))
                    || (stationAId.equals(leg.toStationId()) && stationBId.equals(leg.fromStationId()));
        }
    }
}
