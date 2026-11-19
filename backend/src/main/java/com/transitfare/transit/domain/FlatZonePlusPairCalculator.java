package com.transitfare.transit.domain;

import com.transitfare.transit.contract.LegInput;
import java.util.Optional;

/**
 * 균일 구간 + 정류장 간 요금표(설계 7-1: 니시테츠 버스).
 * 두 정류장이 같은 도심 균일 구간 안이면 균일 요금, 아니면 정류장 쌍 요금. 승차마다 따로 계산한다.
 */
class FlatZonePlusPairCalculator {

    private final FareData data;
    private final StationPairCalculator stationPair;

    FlatZonePlusPairCalculator(FareData data, StationPairCalculator stationPair) {
        this.data = data;
        this.stationPair = stationPair;
    }

    /** 구간 하나 계산. 균일 구간 요금을 먼저 보고, 없으면 쌍 요금. 특급·지정석은 쌍 요금 방식과 같다 */
    LegCharge calculate(LegInput leg) {
        if (!StationPairCalculator.hasStations(leg)) {
            return LegCharge.unknown("승차역·하차역 미확인");
        }
        String operator = leg.operatorCode();
        long from = leg.fromStationId();
        long to = leg.toStationId();
        Optional<Integer> base = data.flatZoneFare(operator, from, to)
                .or(() -> data.pairFare(operator, FareData.BASE, from, to));
        return stationPair.withExtras(leg, base);
    }
}
