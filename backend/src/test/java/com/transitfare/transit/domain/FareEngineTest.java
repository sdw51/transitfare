package com.transitfare.transit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.transitfare.transit.contract.FareStatus;
import com.transitfare.transit.contract.LegFare;
import com.transitfare.transit.contract.LegInput;
import com.transitfare.transit.contract.LegMode;
import com.transitfare.transit.contract.Passengers;
import com.transitfare.transit.contract.SeatType;
import com.transitfare.transit.contract.TrainClass;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 요금 계산 엔진 규칙(설계 7-1, 계획 Task 3). 모든 값은 가상 데이터(실제 요금 아님).
 * 운영사: SUBWAY·JR(거리제), BUS(균일 구간+쌍 요금), PAIR(쌍 요금), OTHER(직접 입력만).
 */
class FareEngineTest {

    // 지하철: 공항선(line 1) 역1 0.0km, 역2 2.0km / 나나쿠마선(line 2) 역3 0.0km, 역4 2.0km
    // JR: 가고시마선(line 10) 역101 0.0, 역102 30.0, 역104 35.7 / 규다이선(line 11) 역104 0.0, 역103 20.0
    final InMemoryFareData data = new InMemoryFareData()
            .withOperator("SUBWAY", FareRuleType.DISTANCE_BAND)
            .withOperator("JR", FareRuleType.DISTANCE_BAND)
            .withOperator("BUS", FareRuleType.FLAT_ZONE_PLUS_PAIR)
            .withOperator("PAIR", FareRuleType.STATION_PAIR)
            .withOperator("OTHER", FareRuleType.MANUAL_ONLY)
            .withKm(1, 1, "0.0").withKm(1, 2, "2.0").withKm(2, 3, "0.0").withKm(2, 4, "2.0")
            .withBand("SUBWAY", FareData.BASE, "3.0", 210).withBand("SUBWAY", FareData.BASE, "7.0", 260)
            .withKm(10, 101, "0.0").withKm(10, 102, "30.0").withKm(10, 104, "35.7")
            .withKm(11, 104, "0.0").withKm(11, 103, "20.0")
            .withBand("JR", FareData.BASE, "30.0", 570).withBand("JR", FareData.BASE, "50.0", 900)
            .withBand("JR", FareData.BASE, "60.0", 1100)
            .withBand("JR", FareData.EXPRESS, "25.0", 520)
            .withBand("JR", FareData.EXPRESS, "50.0", 760)
            .withBand("JR", FareData.EXPRESS, "60.0", 1000)
            .withReservedFee("JR", 530)
            .withZone("BUS", 150, 21L, 22L)
            .withPair("BUS", FareData.BASE, 21, 23, 400)
            .withPair("PAIR", FareData.BASE, 31, 32, 500);

    final FareEngine engine = new FareEngine(data);

    static LegInput transit(String op, Long line, long from, long to) {
        return new LegInput(LegMode.TRANSIT, op, line, from, to, TrainClass.LOCAL, SeatType.UNRESERVED, null, false);
    }

    static LegInput expressReserved(Long line, long from, long to, boolean continuesPreviousTrain) {
        return new LegInput(LegMode.TRANSIT, "JR", line, from, to, TrainClass.EXPRESS, SeatType.RESERVED, null,
                continuesPreviousTrain);
    }

    static LegInput walk() {
        return new LegInput(LegMode.WALK, null, null, null, null, null, null, null, false);
    }

    @Test
    @DisplayName("도보 구간은 요금이 0원이다")
    void walkLegIsFree() {
        List<LegFare> fares = engine.price(List.of(walk()), new Passengers(1));
        assertThat(fares.get(0).status()).isEqualTo(FareStatus.OK);
        assertThat(fares.get(0).partyTotal()).isZero();
    }

    @Test
    @DisplayName("같은 지하철 안 환승은 도보를 사이에 두어도 전체 거리로 한 번 계산한다")
    void subwayTransferWithinOperatorChargedOnceByTotalDistance() {
        List<LegFare> fares = engine.price(List.of(
                transit("SUBWAY", 1L, 1, 2), walk(), transit("SUBWAY", 2L, 3, 4)), new Passengers(1));
        assertThat(fares.get(0).base()).isEqualTo(260);           // 2.0 + 2.0 = 4.0km → 7.0 밴드
        assertThat(fares.get(2).base()).isZero();
        assertThat(fares.get(2).note()).isEqualTo("앞 구간과 합산");
        assertThat(fares.get(0).groupId()).isNotNull().isEqualTo(fares.get(2).groupId());
        assertThat(fares.get(1).groupId()).isNull();               // 도보는 묶음에 들지 않는다
    }

    @Test
    @DisplayName("다른 운영사가 끼면 거리 합산 묶음이 끊긴다")
    void differentOperatorBreaksDistanceGroup() {
        List<LegFare> fares = engine.price(List.of(
                transit("SUBWAY", 1L, 1, 2), transit("PAIR", null, 31, 32), transit("SUBWAY", 2L, 3, 4)),
                new Passengers(1));
        assertThat(fares).extracting(LegFare::base).containsExactly(210, 500, 210);
        assertThat(fares).extracting(LegFare::groupId).containsOnlyNulls();
    }

    @Test
    @DisplayName("JR 특급 지정석은 운임에 특급요금 밴드와 지정석 요금을 더한다")
    void jrExpressAddsExpressBandAndReservedFee() {
        LegInput leg = new LegInput(LegMode.TRANSIT, "JR", 10L, 101L, 102L, TrainClass.EXPRESS, SeatType.RESERVED, null, false);
        LegFare fare = engine.price(List.of(leg), new Passengers(1)).get(0);
        assertThat(fare.base()).isEqualTo(570);       // 30.0km
        assertThat(fare.express()).isEqualTo(760);    // 특급 30.0km → 50.0 밴드
        assertThat(fare.reserved()).isEqualTo(530);
        assertThat(fare.partyTotal()).isEqualTo(1860);
    }

    @Test
    @DisplayName("JR 노선을 갈아타면 거리를 합쳐 한 번 계산한다")
    void jrLineChangeSumsKm() {
        List<LegFare> fares = engine.price(List.of(
                transit("JR", 10L, 101, 104), transit("JR", 11L, 104, 103)), new Passengers(1));
        assertThat(fares.get(0).base()).isEqualTo(1100);  // 35.7 + 20.0 = 55.7km → 60.0 밴드
        assertThat(fares.get(1).base()).isZero();
        assertThat(fares.get(1).note()).isEqualTo("앞 구간과 합산");
    }

    @Test
    @DisplayName("거리제 묶음도 첫 승차역~마지막 하차역 쌍 요금이 있으면 밴드보다 먼저 쓴다")
    void distanceGroupPairFareOverridesBand() {
        data.withPair("JR", FareData.BASE, 101, 102, 500)    // 한 구간 예외
            .withPair("JR", FareData.BASE, 101, 103, 1000);  // 노선을 갈아타는 묶음 전체 예외
        assertThat(engine.price(List.of(transit("JR", 10L, 101, 102)), new Passengers(1)).get(0).base())
                .isEqualTo(500);                              // 밴드라면 570
        List<LegFare> fares = engine.price(List.of(
                transit("JR", 10L, 101, 104), transit("JR", 11L, 104, 103)), new Passengers(1));
        assertThat(fares.get(0).base()).isEqualTo(1000);      // 밴드라면 1100
        assertThat(fares.get(1).base()).isZero();
    }

    @Test
    @DisplayName("요금 데이터가 없으면 0원이 아니라 미확인으로 표시한다")
    void missingFareMarksUnknownNotZeroCharge() {
        List<LegFare> fares = engine.price(List.of(
                transit("PAIR", null, 31, 99),        // 쌍 요금 없음
                transit("SUBWAY", 1L, 1, 99),         // 누적 km 없는 역
                transit("NOPE", null, 1, 2)),         // 모르는 운영사
                new Passengers(2));
        assertThat(fares).extracting(LegFare::status).containsOnly(FareStatus.UNKNOWN);
        assertThat(fares).extracting(LegFare::partyTotal).containsOnly(0);
        assertThat(fares.get(0).note()).contains("운임");
        assertThat(fares.get(1).note()).contains("거리");
        assertThat(fares.get(2).note()).contains("운영사");
    }

    @Test
    @DisplayName("두 정류장이 모두 균일 구간 안이면 균일 요금을 쓴다")
    void flatZoneUsedWhenBothInZone() {
        assertThat(engine.price(List.of(transit("BUS", null, 21, 22)), new Passengers(1)).get(0).base())
                .isEqualTo(150);
    }

    @Test
    @DisplayName("균일 구간 밖이면 정류장 쌍 요금을 쓴다")
    void pairFareUsedOutsideZone() {
        assertThat(engine.price(List.of(transit("BUS", null, 21, 23)), new Passengers(1)).get(0).base())
                .isEqualTo(400);
    }

    @Test
    @DisplayName("회사 간 환승 할인은 두 번째 구간 운임에서 뺀다")
    void transferDiscountSubtractedFromSecondLeg() {
        data.withDiscount("SUBWAY", "BUS", 30);
        List<LegFare> fares = engine.price(List.of(
                transit("SUBWAY", 1L, 1, 2), walk(), transit("BUS", null, 21, 22)), new Passengers(1));
        assertThat(fares.get(0).base()).isEqualTo(210);
        assertThat(fares.get(2).base()).isEqualTo(120);   // 150 - 30
        assertThat(fares.get(2).note()).contains("환승 할인");
    }

    @Test
    @DisplayName("일행 금액은 1인 요금에 어른 수를 곱한다")
    void partyTotalMultipliesByAdults() {
        assertThat(engine.price(List.of(transit("SUBWAY", 1L, 1, 2)), new Passengers(3)).get(0).partyTotal())
                .isEqualTo(630);
    }

    @Test
    @DisplayName("직접 입력 구간은 입력한 금액을 쓴다")
    void manualLegUsesGivenFare() {
        LegInput manual = new LegInput(LegMode.MANUAL, "OTHER", null, null, null, null, null, 1200, false);
        LegFare fare = engine.price(List.of(manual), new Passengers(2)).get(0);
        assertThat(fare.status()).isEqualTo(FareStatus.MANUAL);
        assertThat(fare.base()).isEqualTo(1200);
        assertThat(fare.partyTotal()).isEqualTo(2400);
    }

    @Test
    @DisplayName("같은 열차로 노선이 바뀌는 곳을 지나가면 지정석·특급요금은 열차 하나로 한 번 계산한다")
    void reservedFeeOncePerTrainWhenContinuing() {
        // 博多(101)→久留米(104) 鹿児島本線 35.7km, 久留米→日田(103) 久大本線 20.0km를 같은 특급으로
        List<LegFare> fares = engine.price(List.of(
                expressReserved(10L, 101, 104, false), expressReserved(11L, 104, 103, true)), new Passengers(1));

        assertThat(fares).extracting(LegFare::reserved).containsExactly(530, 0);
        assertThat(fares).extracting(LegFare::express).containsExactly(1000, 0);   // 55.7km → 60.0 밴드
        assertThat(fares).extracting(LegFare::base).containsExactly(1100, 0);      // 운임은 묶음 전체 55.7km
    }

    @Test
    @DisplayName("다른 특급으로 갈아타면 지정석 요금은 열차마다 붙는다")
    void reservedFeePerTrainWhenTransferring() {
        List<LegFare> fares = engine.price(List.of(
                expressReserved(10L, 101, 104, false), expressReserved(11L, 104, 103, false)), new Passengers(1));

        assertThat(fares).extracting(LegFare::reserved).containsExactly(530, 530);
    }

    @Test
    @DisplayName("다른 특급으로 갈아타면 특급요금은 열차마다 따로 계산하고, 운임은 여전히 한 번이다")
    void expressFeePerTrainWhenTransferring() {
        List<LegFare> fares = engine.price(List.of(
                expressReserved(10L, 101, 104, false), expressReserved(11L, 104, 103, false)), new Passengers(1));

        assertThat(fares).extracting(LegFare::express).containsExactly(760, 520);  // 35.7km → 50.0, 20.0km → 25.0
        assertThat(fares).extracting(LegFare::base).containsExactly(1100, 0);
        assertThat(fares.get(1).note()).isEqualTo("앞 구간과 합산");
    }
}
