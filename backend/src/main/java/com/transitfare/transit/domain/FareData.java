package com.transitfare.transit.domain;

import java.math.BigDecimal;
import java.util.Optional;

/**
 * 요금 계산 엔진(FareEngine)이 요금 데이터를 읽는 포트.
 * domain은 Spring·JPA를 모르므로 엔티티 대신 값만 돌려준다. 구현은 repository 패키지의 JpaFareData,
 * 엔진 단위 테스트에서는 메모리 구현(InMemoryFareData).
 * 값이 없으면 Optional.empty — 엔진은 이를 0원이 아니라 "미확인(UNKNOWN)"으로 처리한다.
 * 금액은 모두 어른 1명 기준, 지역 통화의 정수 최소 단위.
 */
public interface FareData {

    /** 기본 운임 (distance_fare_band.kind, pair_fare.kind) */
    String BASE = "BASE";
    /** 특급요금 */
    String EXPRESS = "EXPRESS";

    /** 운영사의 요금 계산 방식. 모르는 운영사면 empty */
    Optional<OperatorRule> operator(String code);

    /** 노선 위 역의 누적 km(line_station.cumulative_km). 역이 그 노선에 없으면 empty */
    Optional<BigDecimal> cumulativeKm(long lineId, long stationId);

    /**
     * 거리 구간 요금(설계 7-1 거리 구간제). km 이상인 밴드 중 max_km가 가장 작은 밴드의 금액.
     * 예: 밴드 3.0→210, 7.0→260이면 3.0km→210, 3.1km→260. km가 모든 밴드보다 크면 empty.
     */
    Optional<Integer> distanceBandFare(String operatorCode, String kind, BigDecimal km);

    /** 역 쌍 요금(설계 7-1 정류장 간 요금표, JR 특정 구간 예외). 양방향 동일: (a, b)와 (b, a) 어느 쪽으로 저장돼 있어도 찾는다 */
    Optional<Integer> pairFare(String operatorCode, String kind, long a, long b);

    /** 균일 구간 요금(설계 7-1 니시테츠 버스 도심 균일 구간). 두 역이 같은 구간에 있을 때만 값이 있다 */
    Optional<Integer> flatZoneFare(String operatorCode, long a, long b);

    /** 지정석 요금(1회 승차당). JR처럼 지정석이 있는 운영사만 */
    Optional<Integer> reservedSeatFee(String operatorCode);

    /** 회사 간 환승 할인(설계 7-1 환승 할인 규칙표). from → to 방향으로 갈아탈 때 두 번째 구간 운임에서 뺄 금액 */
    Optional<Integer> transferDiscount(String fromOperator, String toOperator);
}
