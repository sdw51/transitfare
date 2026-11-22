package com.transitfare.pass.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.transitfare.pass.contract.ProductType;
import com.transitfare.pass.contract.Purchase;
import com.transitfare.pass.contract.TicketLeg;
import com.transitfare.pass.contract.TicketParty;
import com.transitfare.pass.domain.ProductSpec.Coverage;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class TicketOptimizerTest {

    static final LocalDate D1 = LocalDate.of(2026, 12, 1);
    static final LocalDate D2 = D1.plusDays(1);
    static final LocalDate D3 = D1.plusDays(2);
    static final LocalDate D4 = D1.plusDays(3);
    static final LocalDate D5 = D1.plusDays(4);
    static final TicketParty ONE = new TicketParty(1);

    final TicketOptimizer optimizer = new TicketOptimizer();

    @Test
    @DisplayName("승차권을 사는 게 더 비싸면 원래 요금 그대로다")
    void noProductCheaperKeepsBaseline() {
        var legs = List.of(leg(D1, "SUBWAY", 210), leg(D1, "SUBWAY", 210));
        var result = optimizer.optimize(legs, List.of(pass("SUBWAY_1DAY", 640, 1, operator("SUBWAY"))), ONE);
        assertThat(result.baselineTotal()).isEqualTo(420);
        assertThat(result.optimizedTotal()).isEqualTo(420);
        assertThat(result.purchases()).isEmpty();
        assertThat(result.legCoveredBy()).isEmpty();
    }

    @Test
    @DisplayName("하루 요금 합이 1일권보다 비싸면 1일권을 산다")
    void oneDayPassChosenWhenDaySpendExceedsPrice() {
        var legs = List.of(leg(D1, "SUBWAY", 210), leg(D1, "SUBWAY", 210),
                leg(D1, "SUBWAY", 210), leg(D1, "SUBWAY", 210));
        var result = optimizer.optimize(legs, List.of(pass("SUBWAY_1DAY", 640, 1, operator("SUBWAY"))), ONE);
        assertThat(result.optimizedTotal()).isEqualTo(640);
        assertThat(result.purchases()).containsExactly(new Purchase("SUBWAY_1DAY", D1, 640, 1));
        assertThat(result.legCoveredBy()).containsOnlyKeys(0, 1, 2, 3);
    }

    @Test
    @DisplayName("3일권은 시작일부터 연속 3일만 커버한다")
    void threeDayPassCoversConsecutiveDaysOnly() {
        var legs = List.of(leg(D1, "NISHITETSU", 1000), leg(D2, "NISHITETSU", 1000),
                leg(D3, "NISHITETSU", 1000), leg(D5, "NISHITETSU", 1000));
        var result = optimizer.optimize(legs, List.of(pass("BUS_3DAY", 2000, 3, operator("NISHITETSU"))), ONE);
        assertThat(result.optimizedTotal()).isEqualTo(3000);
        assertThat(result.purchases()).containsExactly(new Purchase("BUS_3DAY", D1, 2000, 1));
        assertThat(result.legCoveredBy()).containsOnlyKeys(0, 1, 2);
    }

    @Test
    @DisplayName("특급 미포함 패스는 특급 요금을 커버하지 않는다")
    void passWithoutExpressDoesNotCoverExpressSurcharge() {
        var legs = List.of(new TicketLeg(D1, "JR", null, null, null, true, false, true, 2000, 1500, 0, null));
        var result = optimizer.optimize(legs, List.of(pass("JR_LOCAL", 1000, 1, operator("JR"))), ONE);
        assertThat(result.baselineTotal()).isEqualTo(3500);
        assertThat(result.optimizedTotal()).isEqualTo(2500);
    }

    @Test
    @DisplayName("지정석은 패스의 지정석 횟수 한도까지만 커버한다")
    void reservedSeatLimitRespected() {
        var legs = List.of(reservedExpressLeg(D1), reservedExpressLeg(D1), reservedExpressLeg(D1));
        var jrPass = new ProductSpec("JR_PASS", ProductType.PASS, 3000, 1, 0, true, 2, List.of(operator("JR")));
        var result = optimizer.optimize(legs, List.of(jrPass), ONE);
        assertThat(result.baselineTotal()).isEqualTo(6000);
        assertThat(result.optimizedTotal()).isEqualTo(3500);
    }

    @Test
    @DisplayName("요금 미확인 구간은 승차권으로 커버하지 않는다")
    void unknownLegNeverCovered() {
        var unknown = new TicketLeg(D1, "SUBWAY", null, null, null, false, false, false, 400, 0, 0, null);
        var legs = List.of(leg(D1, "SUBWAY", 500), leg(D1, "SUBWAY", 500), unknown);
        var result = optimizer.optimize(legs, List.of(pass("SUBWAY_1DAY", 640, 1, operator("SUBWAY"))), ONE);
        assertThat(result.optimizedTotal()).isEqualTo(1040);
        assertThat(result.legCoveredBy()).containsOnlyKeys(0, 1);
    }

    @Test
    @DisplayName("왕복권은 같은 두 역 사이를 방향과 상관없이 커버한다")
    void fixedRouteRoundTripCoversBothDirections() {
        var legs = List.of(stationLeg(D1, 1L, 2L, 3000), stationLeg(D2, 2L, 1L, 3000));
        var roundTrip = new ProductSpec("HITA_ROUND", ProductType.FIXED_ROUTE, 5000, 7, 2, false, 0,
                List.of(new Coverage("HITA_BUS", null, 1L, 2L)));
        var result = optimizer.optimize(legs, List.of(roundTrip), ONE);
        assertThat(result.optimizedTotal()).isEqualTo(5000);
        assertThat(result.purchases()).containsExactly(new Purchase("HITA_ROUND", D1, 5000, 1));
        assertThat(result.legCoveredBy()).containsOnlyKeys(0, 1);
    }

    @Test
    @DisplayName("구매 수량은 어른 인원수와 같고 총액도 인원수만큼 곱한다")
    void purchaseCountEqualsAdults() {
        var legs = List.of(leg(D1, "SUBWAY", 300), leg(D1, "SUBWAY", 300),
                leg(D1, "SUBWAY", 300), leg(D1, "SUBWAY", 300));
        var result = optimizer.optimize(legs, List.of(pass("SUBWAY_1DAY", 1000, 1, operator("SUBWAY"))),
                new TicketParty(3));
        assertThat(result.baselineTotal()).isEqualTo(3600);
        assertThat(result.optimizedTotal()).isEqualTo(3000);
        assertThat(result.purchases()).containsExactly(new Purchase("SUBWAY_1DAY", D1, 1000, 3));
    }

    @Test
    @DisplayName("여러 날 패스는 여행 마지막 날을 넘겨도 살 수 있다")
    void multiDayPassMayExtendPastTripEnd() {
        // 여행 D1~D4, 3일권을 D3에 시작하면 D5까지 유효
        var legs = List.of(leg(D1, "SUBWAY", 1000),
                leg(D3, "NISHITETSU", 1000), leg(D3, "NISHITETSU", 1000),
                leg(D4, "NISHITETSU", 1000), leg(D4, "NISHITETSU", 1000));
        var result = optimizer.optimize(legs, List.of(pass("BUS_3DAY", 3000, 3, operator("NISHITETSU"))), ONE);
        assertThat(result.optimizedTotal()).isEqualTo(4000);
        assertThat(result.purchases()).containsExactly(new Purchase("BUS_3DAY", D3, 3000, 1));
    }

    @Test
    @DisplayName("연속 거리제 묶음은 묶음 전 구간이 커버될 때만 절감한다")
    void distanceGroupCoveredOnlyWhenAllLegsCovered() {
        // 공항선(노선 1) → 나나쿠마선(노선 3) 합산 300엔이 첫 구간에, 둘째 구간은 0
        var legs = List.of(groupLeg(1L, 300), groupLeg(3L, 0));
        var airportLineOnly = pass("AIRPORT_LINE", 200, 1, new Coverage("SUBWAY", 1L, null, null));
        var allSubway = pass("SUBWAY_ALL", 200, 1, operator("SUBWAY"));

        var partial = optimizer.optimize(legs, List.of(airportLineOnly), ONE);
        assertThat(partial.optimizedTotal()).isEqualTo(300);
        assertThat(partial.purchases()).isEmpty();

        var full = optimizer.optimize(legs, List.of(allSubway), ONE);
        assertThat(full.optimizedTotal()).isEqualTo(200);
        assertThat(full.legCoveredBy()).containsOnlyKeys(0, 1);
    }

    @Test
    @DisplayName("추천 구매는 시작일 순으로 나온다")
    void purchasesSortedByStartDate() {
        var legs = List.of(leg(D1, "SUBWAY", 300), leg(D1, "SUBWAY", 300), leg(D1, "SUBWAY", 300),
                leg(D2, "NISHITETSU", 400), leg(D2, "NISHITETSU", 400), leg(D2, "NISHITETSU", 400));
        var products = List.of(pass("A_BUS_1DAY", 1000, 1, operator("NISHITETSU")),
                pass("Z_SUBWAY_1DAY", 640, 1, operator("SUBWAY")));
        var result = optimizer.optimize(legs, products, ONE);
        assertThat(result.purchases()).containsExactly(
                new Purchase("Z_SUBWAY_1DAY", D1, 640, 1), new Purchase("A_BUS_1DAY", D2, 1000, 1));
    }

    private static TicketLeg leg(LocalDate date, String operator, int base) {
        return new TicketLeg(date, operator, null, null, null, false, false, true, base, 0, 0, null);
    }

    private static TicketLeg reservedExpressLeg(LocalDate date) {
        return new TicketLeg(date, "JR", null, null, null, true, true, true, 1000, 500, 500, null);
    }

    private static TicketLeg stationLeg(LocalDate date, long from, long to, int base) {
        return new TicketLeg(date, "HITA_BUS", null, from, to, false, false, true, base, 0, 0, null);
    }

    private static TicketLeg groupLeg(long lineId, int base) {
        return new TicketLeg(D1, "SUBWAY", lineId, null, null, false, false, true, base, 0, 0, 7);
    }

    private static ProductSpec pass(String code, int price, int validDays, Coverage... coverage) {
        return new ProductSpec(code, ProductType.PASS, price, validDays, 0, false, 0, List.of(coverage));
    }

    private static Coverage operator(String operatorCode) {
        return new Coverage(operatorCode, null, null, null);
    }
}
