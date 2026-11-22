package com.transitfare.transit.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.transitfare.common.error.BusinessException;
import com.transitfare.transit.error.TransitErrorCode;
import com.transitfare.transit.repository.PairFareRepository;
import com.transitfare.transit.repository.StationRepository;
import com.transitfare.transit.usecase.dto.GtfsImportResult;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

/**
 * GTFS 요금 가져오기. GTFS zip은 테스트 안에서 만든다(GtfsZip). 가상 데이터:
 * 정류장 天神(S1·S2 두 승강장, 구역 Z1), 博多駅(S3, Z3), キャナルシティ(S4, Z4)
 * 요금 F1 210엔 Z1→Z3, F2 260엔 Z1→Z4
 */
@SpringBootTest
@Transactional
@Sql("/transit/gtfs-test.sql")
class ImportGtfsFaresUseCaseTest {

    static final String SOURCE = "https://example.test/gtfs";

    static final String STOPS = """
            stop_id,stop_name,stop_lat,stop_lon,zone_id
            S1,天神,33.59,130.40,Z1
            S2,天神,33.59,130.40,Z1
            S3,博多駅,33.59,130.42,Z3
            S4,キャナルシティ,33.59,130.41,Z4
            """;
    static final String FARE_ATTRIBUTES = """
            fare_id,price,currency_type,payment_method,transfers
            F1,210,JPY,0,0
            F2,260,JPY,0,0
            """;
    static final String FARE_RULES = """
            fare_id,route_id,origin_id,destination_id,contains_id
            F1,,Z1,Z3,
            F2,,Z1,Z4,
            """;

    @Autowired
    ImportGtfsFaresUseCase importGtfsFaresUseCase;
    @Autowired
    StationRepository stationRepository;
    @Autowired
    PairFareRepository pairFareRepository;

    GtfsImportResult importFeed(String stops, String fareAttributes, String fareRules) {
        return importGtfsFaresUseCase.execute("TEST_BUS", SOURCE, GtfsZip.of(stops, fareAttributes, fareRules));
    }

    long stationId(String nameJa) {
        return stationRepository.findByOperatorCodeAndNameJa("TEST_BUS", nameJa).orElseThrow().getId();
    }

    @Test
    @DisplayName("정류장과 구역 간(origin·destination) 요금을 가져온다. 같은 이름 승강장은 한 역으로 합친다")
    void importsStopsAndOdFares() {
        GtfsImportResult result = importFeed(STOPS, FARE_ATTRIBUTES, FARE_RULES);

        assertThat(result.stationsUpserted()).isEqualTo(3);          // 天神 두 승강장 → 한 역
        assertThat(result.faresUpserted()).isEqualTo(2);
        assertThat(result.skipped()).isEmpty();
        assertThat(pairFareRepository.findPair("TEST_BUS", "BASE", stationId("博多駅"), stationId("天神")))
                .hasValueSatisfying(f -> {
                    assertThat(f.getAmount()).isEqualTo(210);
                    assertThat(f.getSourceUrl()).isEqualTo(SOURCE);
                    assertThat(f.getVerifiedOn()).isEqualTo(LocalDate.now());
                });
        assertThat(stationRepository.findByOperatorCodeAndNameJa("TEST_BUS", "天神"))
                .hasValueSatisfying(s -> assertThat(s.getNameKo()).isNull());   // 한국어 이름은 비움
    }

    @Test
    @DisplayName("같은 파일을 다시 가져와도 행이 늘지 않고, 바뀐 가격만 갱신한다")
    void reimportIsIdempotent() {
        importFeed(STOPS, FARE_ATTRIBUTES, FARE_RULES);
        GtfsImportResult again = importFeed(STOPS, FARE_ATTRIBUTES.replace("F1,210", "F1,220"), FARE_RULES);

        assertThat(again.stationsUpserted()).isEqualTo(3);
        assertThat(again.faresUpserted()).isEqualTo(2);
        assertThat(stationRepository.findByOperatorCodeOrderById("TEST_BUS")).hasSize(3);
        assertThat(pairFareRepository.findAll()).filteredOn(f -> f.getOperatorCode().equals("TEST_BUS")).hasSize(2);
        assertThat(pairFareRepository.findPair("TEST_BUS", "BASE", stationId("天神"), stationId("博多駅")))
                .hasValueSatisfying(f -> assertThat(f.getAmount()).isEqualTo(220));
    }

    @Test
    @DisplayName("구역 방식 요금 규칙이나 요금 파일이 없으면 정류장만 넣고 skipped에 사유를 남긴다")
    void zoneBasedFeedReportedAsSkipped() {
        String zoneRules = """
                fare_id,route_id,origin_id,destination_id,contains_id
                F1,R1,,,Z1
                """;
        GtfsImportResult zoneBased = importFeed(STOPS, FARE_ATTRIBUTES, zoneRules);
        assertThat(zoneBased.stationsUpserted()).isEqualTo(3);
        assertThat(zoneBased.faresUpserted()).isZero();
        assertThat(zoneBased.skipped()).anyMatch(reason -> reason.contains("구역 방식"));

        GtfsImportResult noFares = importFeed(STOPS, null, null);
        assertThat(noFares.faresUpserted()).isZero();
        assertThat(noFares.skipped()).anyMatch(reason -> reason.contains("요금 파일 없음"));
    }

    @Test
    @DisplayName("zip이 아니거나 stops.txt가 없으면 400 TRANSIT_GTFS_INVALID")
    void invalidFeedRejected() {
        assertThatThrownBy(() -> importGtfsFaresUseCase.execute("TEST_BUS", SOURCE,
                new ByteArrayInputStream("not a zip".getBytes())))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(TransitErrorCode.TRANSIT_GTFS_INVALID);
        assertThatThrownBy(() -> importFeed(null, FARE_ATTRIBUTES, FARE_RULES))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(TransitErrorCode.TRANSIT_GTFS_INVALID);
    }
}
