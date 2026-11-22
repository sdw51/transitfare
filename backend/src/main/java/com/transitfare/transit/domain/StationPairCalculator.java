package com.transitfare.transit.domain;

import com.transitfare.transit.contract.LegInput;
import com.transitfare.transit.contract.SeatType;
import com.transitfare.transit.contract.TrainClass;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 정류장 간 요금표(설계 7-1: 니시테츠 전철·고속버스 히타호·쇼와 버스·모노레일).
 * 승차마다 따로 계산한다: 운임 = BASE 쌍 요금, 특급이면 EXPRESS 쌍 요금, 지정석이면 지정석 요금을 더한다.
 */
class StationPairCalculator {

    private final FareData data;

    StationPairCalculator(FareData data) {
        this.data = data;
    }

    /** 구간 하나 계산. 승차역·하차역이 없거나 요금이 하나라도 없으면 UNKNOWN */
    LegCharge calculate(LegInput leg) {
        if (!hasStations(leg)) {
            return LegCharge.unknown("승차역·하차역 미확인");
        }
        return withExtras(leg, data.pairFare(leg.operatorCode(), FareData.BASE, leg.fromStationId(), leg.toStationId()));
    }

    /**
     * 운임은 받은 값으로 하고 특급요금·지정석 요금을 이 방식대로 더한다.
     * 균일 구간 계산기(FlatZonePlusPairCalculator)도 운임만 바꿔서 이걸 쓴다.
     */
    LegCharge withExtras(LegInput leg, Optional<Integer> base) {
        String operator = leg.operatorCode();
        List<String> missing = new ArrayList<>();
        if (base.isEmpty()) {
            missing.add("운임");
        }
        int express = 0;
        if (leg.trainClass() == TrainClass.EXPRESS) {
            Optional<Integer> e = data.pairFare(operator, FareData.EXPRESS, leg.fromStationId(), leg.toStationId());
            if (e.isEmpty()) {
                missing.add("특급요금");
            }
            express = e.orElse(0);
        }
        int reserved = 0;
        if (leg.seatType() == SeatType.RESERVED) {
            Optional<Integer> r = data.reservedSeatFee(operator);
            if (r.isEmpty()) {
                missing.add("지정석 요금");
            }
            reserved = r.orElse(0);
        }
        return missing.isEmpty() ? LegCharge.ok(base.get(), express, reserved, null) : LegCharge.missing(missing);
    }

    static boolean hasStations(LegInput leg) {
        return leg.fromStationId() != null && leg.toStationId() != null;
    }
}
