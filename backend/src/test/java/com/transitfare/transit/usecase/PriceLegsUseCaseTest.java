package com.transitfare.transit.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.transitfare.common.error.BusinessException;
import com.transitfare.transit.contract.FareStatus;
import com.transitfare.transit.contract.LegFare;
import com.transitfare.transit.contract.LegInput;
import com.transitfare.transit.contract.LegMode;
import com.transitfare.transit.contract.Passengers;
import com.transitfare.transit.contract.PricingResult;
import com.transitfare.transit.contract.SeatType;
import com.transitfare.transit.contract.TrainClass;
import com.transitfare.transit.error.TransitErrorCode;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

/**
 * 구간 요금 계산 유스케이스. 실제 DB(JpaFareData) + 요금 계산 엔진으로 계산한다.
 * 테스트 데이터(pricing-test.sql): 지역 TEST의 거리제 지하철·직접 입력 운영사, 다른 지역 TEST2의 버스(가상 요금).
 */
@SpringBootTest
@Transactional
@Sql("/transit/pricing-test.sql")
class PriceLegsUseCaseTest {

    @Autowired
    PriceLegsUseCase priceLegsUseCase;

    static LegInput subway(long from, long to) {
        return new LegInput(LegMode.TRANSIT, "TEST_SUBWAY", 1L, from, to, TrainClass.LOCAL, SeatType.UNRESERVED, null);
    }

    @Test
    @DisplayName("구간 요금을 계산하고 일행 총액을 더한다")
    void pricesLegsAndSumsParty() {
        LegInput manual = new LegInput(LegMode.MANUAL, "TEST_OTHER", null, null, null, null, null, 1000);
        PricingResult result = priceLegsUseCase.execute("TEST", List.of(subway(1, 2), manual), new Passengers(2));

        assertThat(result.legs()).extracting(LegFare::base).containsExactly(210, 1000);
        assertThat(result.partyTotal()).isEqualTo((210 + 1000) * 2);
        assertThat(result.hasUnknown()).isFalse();
    }

    @Test
    @DisplayName("미확인 구간이 있으면 hasUnknown이 true이고 총액에는 확인된 금액만 더한다")
    void unknownLegSetsHasUnknown() {
        // 역 9는 노선에 없어서 거리를 모른다
        PricingResult result = priceLegsUseCase.execute("TEST", List.of(subway(1, 2), subway(2, 9)), new Passengers(1));

        assertThat(result.hasUnknown()).isTrue();
        assertThat(result.legs().get(1).status()).isEqualTo(FareStatus.UNKNOWN);
        assertThat(result.partyTotal()).isZero();   // 같은 운영사 묶음이라 둘 다 미확인
    }

    @Test
    @DisplayName("다른 지역 운영사가 섞이면 TRANSIT_OPERATOR_REGION_MISMATCH")
    void operatorFromOtherRegionRejected() {
        LegInput otherRegion = new LegInput(LegMode.TRANSIT, "TEST2_BUS", null, 21L, 22L,
                TrainClass.LOCAL, SeatType.UNRESERVED, null);

        assertThatThrownBy(() -> priceLegsUseCase.execute("TEST", List.of(subway(1, 2), otherRegion), new Passengers(1)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(TransitErrorCode.TRANSIT_OPERATOR_REGION_MISMATCH);
    }

    @Test
    @DisplayName("인원이 0명이면 TRANSIT_INVALID_PASSENGERS")
    void zeroAdultsRejected() {
        assertThatThrownBy(() -> priceLegsUseCase.execute("TEST", List.of(subway(1, 2)), new Passengers(0)))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getErrorCode())
                .isEqualTo(TransitErrorCode.TRANSIT_INVALID_PASSENGERS);
    }
}
