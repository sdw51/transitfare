package com.transitfare.transit.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.transitfare.common.ModuleFlywayConfig;
import com.transitfare.transit.domain.FareData;
import com.transitfare.transit.domain.FareRuleType;
import java.math.BigDecimal;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.jdbc.Sql;

/**
 * JpaFareData가 요금 테이블을 계획대로 읽는지 확인한다.
 * 모듈별 Flyway(ModuleFlywayConfig)로 실제 V1 스키마를 만들고, 테스트 전용 지역·운영사·역(fare-data-test.sql)을 넣는다.
 * 테스트 데이터는 지역 TEST, id 1~99라 실제 시드(JP-FUKUOKA, id 100000~)와 겹치지 않는다.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE) // application-test.yml의 H2를 그대로 사용
@Import({ModuleFlywayConfig.class, JpaFareData.class})
@Sql("/transit/fare-data-test.sql")
class JpaFareDataTest {

    @Autowired
    FareData fareData;

    @Test
    @DisplayName("거리 구간 요금은 km를 덮는 가장 작은 밴드를 고른다")
    void distanceBandFareReturnsSmallestBandCoveringKm() {
        assertThat(fareData.distanceBandFare("TEST_DIST", FareData.BASE, new BigDecimal("3.0"))).contains(210);
        assertThat(fareData.distanceBandFare("TEST_DIST", FareData.BASE, new BigDecimal("3.1"))).contains(260);
        assertThat(fareData.distanceBandFare("TEST_DIST", FareData.BASE, new BigDecimal("7.0"))).contains(260);
        // 가장 큰 밴드보다 멀면 요금 없음(미확인)
        assertThat(fareData.distanceBandFare("TEST_DIST", FareData.BASE, new BigDecimal("7.1"))).isEmpty();
    }

    @Test
    @DisplayName("역 쌍 요금은 방향과 관계없이 같다")
    void pairFareIsSymmetric() {
        assertThat(fareData.pairFare("TEST_PAIR", FareData.BASE, 11, 12)).contains(500);
        assertThat(fareData.pairFare("TEST_PAIR", FareData.BASE, 12, 11)).contains(500);
        // 종류(kind)가 다르면 다른 요금
        assertThat(fareData.pairFare("TEST_PAIR", FareData.EXPRESS, 11, 12)).isEmpty();
    }

    @Test
    @DisplayName("균일 구간 요금은 두 정류장이 모두 구간 안에 있을 때만 있다")
    void flatZoneFareOnlyWhenBothStationsInZone() {
        assertThat(fareData.flatZoneFare("TEST_BUS", 21, 22)).contains(150);
        assertThat(fareData.flatZoneFare("TEST_BUS", 21, 23)).isEmpty(); // 23은 구간 밖
    }

    @Test
    @DisplayName("운영사 규칙, 누적 km, 지정석 요금, 환승 할인을 읽는다")
    void readsOperatorKmFeeAndDiscount() {
        assertThat(fareData.operator("TEST_DIST")).hasValueSatisfying(r ->
                assertThat(r.ruleType()).isEqualTo(FareRuleType.DISTANCE_BAND));
        assertThat(fareData.operator("NONE")).isEmpty();
        assertThat(fareData.cumulativeKm(1, 2)).contains(new BigDecimal("2.0"));
        assertThat(fareData.cumulativeKm(1, 21)).isEmpty(); // 그 노선에 없는 역
        assertThat(fareData.reservedSeatFee("TEST_DIST")).contains(530);
        assertThat(fareData.transferDiscount("TEST_DIST", "TEST_BUS")).contains(30);
        assertThat(fareData.transferDiscount("TEST_BUS", "TEST_DIST")).isEmpty(); // 방향이 있다
    }
}
