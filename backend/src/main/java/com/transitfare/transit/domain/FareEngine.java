package com.transitfare.transit.domain;

import com.transitfare.transit.contract.LegFare;
import com.transitfare.transit.contract.LegInput;
import com.transitfare.transit.contract.Passengers;
import java.util.ArrayList;
import java.util.List;

/**
 * 요금 계산 엔진(설계 7-1 요금 모델). 순서만 정하고 계산은 요금 방식별 계산기가 한다.
 *
 * <pre>
 * 예) 지하철 博多→祇園, 도보, 지하철 天神→薬院, 버스 天神→博多
 * 1. 단위로 자르기   [지하철, 도보, 지하철] [버스]
 * 2. 단위별 계산     [260, 0, 0(앞 구간과 합산)] [150]  → 이어 붙임
 * 3. 환승 할인       버스 150 → 120 (지하철→버스 할인 30이 있을 때)
 * 4. 인원 곱하기     partyTotal = (운임 + 특급 + 지정석) × 어른 수
 * </pre>
 */
public class FareEngine {

    private final DistanceBandCalculator distanceBand;
    private final StationPairCalculator stationPair;
    private final FlatZonePlusPairCalculator flatZonePlusPair;
    private final ManualFareCalculator manual;
    private final TransferDiscountApplier transferDiscount;
    private final FareData data;

    public FareEngine(FareData data) {
        this.data = data;
        this.distanceBand = new DistanceBandCalculator(data);
        this.stationPair = new StationPairCalculator(data);
        this.flatZonePlusPair = new FlatZonePlusPairCalculator(data, stationPair);
        this.manual = new ManualFareCalculator();
        this.transferDiscount = new TransferDiscountApplier(data);
    }

    /**
     * 구간 목록의 요금을 계산한다.
     *
     * @param legs       구간 목록(순서대로)
     * @param passengers 인원(검증은 유스케이스 몫 — 여기서는 곱하기만)
     * @return 입력과 같은 길이·순서의 구간 요금
     */
    public List<LegFare> price(List<LegInput> legs, Passengers passengers) {
        // 1. 계산 단위로 자르기
        List<FareUnit> units = FareUnit.split(legs, data);

        // 2. 단위마다 계산기에 맡기고 결과를 이어 붙인다 (단위는 이어진 구간이라 순서가 유지된다)
        List<LegCharge> charges = new ArrayList<>();
        for (FareUnit unit : units) {
            charges.addAll(calculate(unit));
        }

        // 3. 회사 간 환승 할인
        charges = transferDiscount.apply(legs, charges);

        // 4. 인원을 곱해 공개 결과로
        List<LegFare> result = new ArrayList<>();
        for (int i = 0; i < legs.size(); i++) {
            result.add(charges.get(i).toLegFare(i, legs.get(i).operatorCode(), passengers));
        }
        return result;
    }

    /** 계산기 고르기: 먼저 구간 종류(LegMode), TRANSIT이면 운영사 요금 방식(FareRuleType). 돌려주는 요금 수 = 단위 안 구간 수 */
    private List<LegCharge> calculate(FareUnit unit) {
        switch (unit.first().mode()) {
            case WALK:
                return List.of(LegCharge.free());
            case MANUAL:
                return List.of(manual.calculate(unit.first()));
            case TRANSIT:
                break;
        }
        if (unit.rule() == null) {
            return List.of(LegCharge.unknown("운영사 미확인"));
        }
        return switch (unit.rule().ruleType()) {
            case DISTANCE_BAND -> distanceBand.calculate(unit.legs(), unit.groupId());
            case STATION_PAIR -> List.of(stationPair.calculate(unit.first()));
            case FLAT_ZONE_PLUS_PAIR -> List.of(flatZonePlusPair.calculate(unit.first()));
            case MANUAL_ONLY -> List.of(LegCharge.unknown("직접 입력이 필요한 운영사"));
        };
    }
}
