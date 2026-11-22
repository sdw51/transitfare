package com.transitfare.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.transitfare.pass.PassFacade;
import com.transitfare.pass.contract.OptimizationResult;
import com.transitfare.pass.contract.Purchase;
import com.transitfare.pass.contract.TicketLeg;
import com.transitfare.pass.contract.TicketParty;
import com.transitfare.transit.TransitFacade;
import com.transitfare.transit.contract.FareStatus;
import com.transitfare.transit.contract.LegFare;
import com.transitfare.transit.contract.LegInput;
import com.transitfare.transit.contract.LegMode;
import com.transitfare.transit.contract.LineRef;
import com.transitfare.transit.contract.Passengers;
import com.transitfare.transit.contract.PricingResult;
import com.transitfare.transit.contract.SeatType;
import com.transitfare.transit.contract.StationView;
import com.transitfare.transit.contract.TrainClass;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.function.ToIntFunction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 시드 요금 데이터(V100)를 공식 자료의 실제 요금과 대조한다. 금액은 어른 1명 기준.
 * 기대값과 출처는 docs/data-sources.md에도 정리되어 있다.
 */
@SpringBootTest
@Tag("golden")
class GoldenFareTest {

    private static final String REGION = "JP-FUKUOKA";
    private static final String SUBWAY = "FUKUOKA_SUBWAY";
    private static final String JR = "JR_KYUSHU";
    private static final String BUS = "NISHITETSU_BUS";
    private static final String HIGHWAY_BUS = "NISHITETSU_HIGHWAY_BUS";

    @Autowired
    TransitFacade transitFacade;

    @Autowired
    PassFacade passFacade;

    private int nextGroupOffset = 1;

    // https://subway.city.fukuoka.lg.jp/fare/futuken/ — 1구간(3km까지) 210엔. 博多~天神 2.5km
    // https://subway.city.fukuoka.lg.jp/fare/pdf/subwayA4Side.pdf — 역간 영업거리
    @Test
    @DisplayName("지하철 하카타→덴진은 1구간 210엔이다")
    void subwayHakataToTenjin() {
        PricingResult result = price(ride(SUBWAY, "공항선", "博多", "天神"));

        assertThat(result.hasUnknown()).isFalse();
        assertThat(result.partyTotal()).isEqualTo(210);
    }

    // https://subway.city.fukuoka.lg.jp/fare/futuken/ — 2구간(3km 초과 7km까지) 260엔. 博多~福岡空港 3.3km
    @Test
    @DisplayName("지하철 하카타→후쿠오카공항은 2구간 260엔이다")
    void subwayHakataToFukuokaAirport() {
        PricingResult result = price(ride(SUBWAY, "공항선", "博多", "福岡空港"));

        assertThat(result.hasUnknown()).isFalse();
        assertThat(result.partyTotal()).isEqualTo(260);
    }

    // https://subway.city.fukuoka.lg.jp/fare/pdf/renrakuA4Side.pdf (2026-04-01 요금표) — 福岡空港~薬院 2구간 260엔
    //   (계획의 하카타→야쿠인은 2023년 나나쿠마선 하카타 연장 뒤 환승 없는 직통이라 후쿠오카공항→야쿠인으로 바꿈, 사용자 결정)
    //   공항선 福岡空港→博多 3.3km + 나나쿠마선 博多→薬院 2.8km = 6.1km를 한 번에 계산
    @Test
    @DisplayName("지하철 공항선에서 나나쿠마선으로 갈아타면 전체 거리로 한 번 계산한다")
    void subwayTransferAirportLineToNanakumaLine() {
        PricingResult result = price(
                ride(SUBWAY, "공항선", "福岡空港", "博多"),
                walk(),
                ride(SUBWAY, "나나쿠마선", "博多", "薬院"));

        assertThat(result.hasUnknown()).isFalse();
        assertThat(result.partyTotal()).isEqualTo(260);
        assertThat(result.legs().get(0).base()).isEqualTo(260);
        assertThat(result.legs().get(2).base()).isZero();
        assertThat(result.legs().get(2).groupId()).isEqualTo(result.legs().get(0).groupId());
    }

    // https://subway.city.fukuoka.lg.jp/fare/pdf/renrakuA4Side.pdf — 지하철 3구간 역(博多)에서 JR 筑前前原까지 연락운임 640엔
    //   = 지하철 博多~姪浜 9.8km 300엔 + JR 姪浜~筑前前原 12.7km 340엔(같은 표의 姪浜 행)
    @Test
    @DisplayName("지하철+JR 지쿠히선 하카타→지쿠젠마에바루는 두 회사 운임을 더해 640엔이다")
    void subwayPlusJrHakataToChikuzenMaebaru() {
        PricingResult result = price(
                ride(SUBWAY, "공항선", "博多", "姪浜"),
                ride(JR, "지쿠히선", "姪浜", "筑前前原"));

        assertThat(result.hasUnknown()).isFalse();
        assertThat(result.legs().get(0).base()).isEqualTo(300);
        assertThat(result.legs().get(1).base()).isEqualTo(340);
        assertThat(result.partyTotal()).isEqualTo(640);
    }

    // https://www.jrkyushu.co.jp/trains/yufuinnomori/ — 博多~日田 ゆふいんの森 지정석 4,160엔 = 운임 1,930 + 특급 2,230(1,730+관광열차 500)
    // https://www.jrkyushu.co.jp/railway/ticket/rule/transport/ (여객영업규칙 第77条の5, 第81条の5) —
    //   博多~久留米 영업 35.7km + 久留米~日田 의제 52.4km = 운임계산키로 88.1km → 81~90km 1,930엔
    @Test
    @DisplayName("JR 하카타→히타 보통열차는 가고시마본선과 규다이본선 거리를 합쳐 1,930엔이다")
    void jrHakataToHitaLocal() {
        PricingResult result = price(
                ride(JR, "가고시마본선", "博多", "久留米"),
                ride(JR, "규다이본선", "久留米", "日田"));

        assertThat(result.hasUnknown()).isFalse();
        assertThat(result.partyTotal()).isEqualTo(1930);
    }

    // 운임 1,930엔은 위와 같음.
    // https://www.jrkyushu.co.jp/railway/kaitei/pdf/241217_unchinkaitei_guidebook.pdf — 재래선 특급(통상기) 100km까지
    //   지정석 1,730엔 = 자유석 1,200엔 + 530엔. 특급 유후 등 일반 특급 기준(ゆふいんの森의 +500엔은 넣지 않음)
    @Test
    @DisplayName("JR 하카타→히타 특급 지정석은 운임 1,930 + 특급 1,200 + 지정석 530 = 3,660엔이다")
    void jrHakataToHitaExpressReserved() {
        PricingResult result = price(
                express(JR, "가고시마본선", "博多", "久留米"),
                express(JR, "규다이본선", "久留米", "日田"));

        assertThat(result.hasUnknown()).isFalse();
        assertThat(sum(result, LegFare::base)).isEqualTo(1930);
        assertThat(sum(result, LegFare::express)).isEqualTo(1200);
        assertThat(sum(result, LegFare::reserved)).isEqualTo(530);
        assertThat(result.partyTotal()).isEqualTo(3660);
    }

    // https://www.jrkyushu.co.jp/railway/ticket/rule/transport/ (여객영업규칙 第77条の5, 第81条の5) —
    //   博多~久留米 영업 35.7km + 久留米~うきは 의제 33.0km = 68.7km → 61~70km 1,510엔
    @Test
    @DisplayName("JR 하카타→우키하는 1,510엔이다")
    void jrHakataToUkiha() {
        PricingResult result = price(
                ride(JR, "가고시마본선", "博多", "久留米"),
                ride(JR, "규다이본선", "久留米", "うきは"));

        assertThat(result.hasUnknown()).isFalse();
        assertThat(result.partyTotal()).isEqualTo(1510);
    }

    // https://www.nishitetsu.jp/bus/highwaybus/rosen/hita/ — 운임표: 博多バスターミナル~日田バスターミナル 1,980엔
    @Test
    @DisplayName("고속버스 히타호 하카타BT→히타BT는 1,980엔이다")
    void highwayBusHitaGoHakataToHita() {
        PricingResult result = price(ride(HIGHWAY_BUS, null, "博多バスターミナル", "日田バスターミナル"));

        assertThat(result.hasUnknown()).isFalse();
        assertThat(result.partyTotal()).isEqualTo(1980);
    }

    // https://www.nishitetsu.jp/bus/rosen/150/ — 후쿠오카 도심 150엔 균일 구간(博多駅前Ａ, 天神大丸前 모두 구역 안)
    @Test
    @DisplayName("니시테츠 버스 도심 균일 구간 안 하카타역앞→덴진다이마루앞은 150엔이다")
    void nishitetsuBusInsideFlatZone() {
        PricingResult result = price(ride(BUS, null, "博多駅前Ａ", "天神大丸前"));

        assertThat(result.hasUnknown()).isFalse();
        assertThat(result.partyTotal()).isEqualTo(150);
    }

    // https://www.sunqpass.jp/information/info/2024-09-09/JP_20241001price.pdf — SUNQ 북큐슈 3일권(디지털) 10,000엔
    // https://www.sunqpass.jp/area/ — 북큐슈 구역 노선 "福岡･福岡空港～日田"(히타호) 포함
    // https://www.nishitetsu.jp/bus/highwaybus/rosen/hita/ — 博多BT~日田BT 1,980엔
    //   3일 동안 매일 히타호 왕복: 개별 1,980 × 6 = 11,880엔 > SUNQ 10,000엔
    @Test
    @DisplayName("히타호를 3일 연속 왕복하면 SUNQ 3일권이 추천되고 10,000엔이 된다")
    void sunqPassCoversHitaGo() {
        LocalDate day1 = LocalDate.of(2026, 12, 1);
        List<TicketLeg> legs = new ArrayList<>();
        for (int day = 0; day < 3; day++) {
            LocalDate date = day1.plusDays(day);
            legs.addAll(ticketLegs(date, ride(HIGHWAY_BUS, null, "博多バスターミナル", "日田バスターミナル")));
            legs.addAll(ticketLegs(date, ride(HIGHWAY_BUS, null, "日田バスターミナル", "博多バスターミナル")));
        }

        OptimizationResult result = passFacade.optimize(REGION, legs, new TicketParty(1), List.of());

        assertThat(result.baselineTotal()).isEqualTo(11_880);
        assertThat(result.optimizedTotal()).isEqualTo(10_000);
        assertThat(result.purchases()).extracting(Purchase::productCode).containsExactly("SUNQ_NORTH_3DAY");
    }

    // https://www.jrkyushu.co.jp/english/railpass/railpass.html — JR 큐슈 레일패스 북큐슈 3일 15,000엔,
    //   특급 포함, 무료 지정석 6회(Northern Kyushu "Up to 6 times")
    //   3일 동안 매일 하카타↔히타 특급 지정석 왕복: 3,660 × 6 = 21,960엔, 지정석 6회 → 패스 15,000엔
    @Test
    @DisplayName("하카타↔히타 특급 지정석을 3일 왕복하면 JR 큐슈 레일패스 북큐슈 3일권이 특급·지정석까지 커버한다")
    void jrPassCoversHakataHitaExpress() {
        LocalDate day1 = LocalDate.of(2026, 12, 1);
        List<TicketLeg> legs = new ArrayList<>();
        for (int day = 0; day < 3; day++) {
            LocalDate date = day1.plusDays(day);
            legs.addAll(ticketLegs(date,
                    express(JR, "가고시마본선", "博多", "久留米"),
                    express(JR, "규다이본선", "久留米", "日田")));
            legs.addAll(ticketLegs(date,
                    express(JR, "규다이본선", "日田", "久留米"),
                    express(JR, "가고시마본선", "久留米", "博多")));
        }

        OptimizationResult result = passFacade.optimize(REGION, legs, new TicketParty(1), List.of());

        assertThat(result.baselineTotal()).isEqualTo(21_960);
        assertThat(result.optimizedTotal()).isEqualTo(15_000);
        assertThat(result.purchases()).extracting(Purchase::productCode)
                .containsExactly("JR_KYUSHU_RAIL_PASS_NORTH_3DAY");
    }

    private PricingResult price(LegInput... legs) {
        return transitFacade.priceLegs(REGION, List.of(legs), new Passengers(1));
    }

    private LegInput ride(String operator, String lineNameKo, String from, String to) {
        return leg(operator, lineNameKo, from, to, TrainClass.LOCAL, SeatType.UNRESERVED);
    }

    private LegInput express(String operator, String lineNameKo, String from, String to) {
        return leg(operator, lineNameKo, from, to, TrainClass.EXPRESS, SeatType.RESERVED);
    }

    private LegInput leg(String operator, String lineNameKo, String from, String to,
                         TrainClass trainClass, SeatType seatType) {
        StationView fromStation = station(operator, from);
        Long lineId = lineNameKo == null ? null : fromStation.lines().stream()
                .filter(line -> line.nameKo().equals(lineNameKo))
                .map(LineRef::lineId)
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(from + "에 " + lineNameKo + " 노선 없음"));
        return new LegInput(LegMode.TRANSIT, operator, lineId, fromStation.id(), station(operator, to).id(),
                trainClass, seatType, null, false);
    }

    private LegInput walk() {
        return new LegInput(LegMode.WALK, null, null, null, null, null, null, null, false);
    }

    private StationView station(String operator, String nameJa) {
        return transitFacade.searchStations(REGION, operator, nameJa).stream()
                .filter(station -> station.nameJa().equals(nameJa))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException(operator + " 역 없음: " + nameJa));
    }

    private int sum(PricingResult result, ToIntFunction<LegFare> amount) {
        return result.legs().stream().mapToInt(amount).sum();
    }

    /** 같은 날의 한 번 이동(편도)을 요금 계산해 TicketLeg로 바꾼다. 변환은 docs/contracts.md의 itinerary 변환 표 그대로. */
    private List<TicketLeg> ticketLegs(LocalDate date, LegInput... inputs) {
        PricingResult priced = price(inputs);
        // groupId는 priceLegs 호출마다 새로 매겨지므로, 여러 번 호출한 결과를 합칠 때 겹치지 않게 호출마다 다른 값으로 바꾼다
        int groupOffset = nextGroupOffset++ * 1000;
        List<TicketLeg> legs = new ArrayList<>();
        for (int i = 0; i < inputs.length; i++) {
            LegInput input = inputs[i];
            LegFare fare = priced.legs().get(i);
            if (input.mode() == LegMode.WALK) {
                continue;
            }
            legs.add(new TicketLeg(date, input.operatorCode(), input.lineId(),
                    input.fromStationId(), input.toStationId(),
                    input.trainClass() == TrainClass.EXPRESS,
                    input.seatType() == SeatType.RESERVED,
                    fare.status() == FareStatus.OK,
                    fare.base(), fare.express(), fare.reserved(),
                    fare.groupId() == null ? null : groupOffset + fare.groupId()));
        }
        return legs;
    }
}
