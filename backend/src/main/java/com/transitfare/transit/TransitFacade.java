package com.transitfare.transit;

import com.transitfare.transit.contract.LegInput;
import com.transitfare.transit.contract.OperatorView;
import com.transitfare.transit.contract.Passengers;
import com.transitfare.transit.contract.PricingResult;
import com.transitfare.transit.contract.RegionView;
import com.transitfare.transit.contract.StationView;
import java.util.List;

/**
 * transit 모듈의 유일한 공개 진입점. 다른 모듈(itinerary, directions)은 이 인터페이스와 contract 패키지만 쓴다.
 * 메서드는 태스크가 진행되며 늘어난다: matchStep(Task 10-A).
 */
public interface TransitFacade {

    /**
     * 구간 목록의 요금을 계산한다(운영사별 요금 규칙, 거리제 묶음 합산, 환승 할인 포함).
     * 데이터에 없는 구간은 UNKNOWN(0원 아님). 인원 0명 → TRANSIT_INVALID_PASSENGERS,
     * 없는 지역 → TRANSIT_REGION_NOT_FOUND, 다른 지역 운영사 → TRANSIT_OPERATOR_REGION_MISMATCH.
     */
    PricingResult priceLegs(String regionCode, List<LegInput> legs, Passengers passengers);

    /** 지역의 역·정류장을 이름(일본어·한국어·별칭 부분 일치)으로 검색한다. 최대 30개. 없는 지역이면 TRANSIT_REGION_NOT_FOUND */
    List<StationView> searchStations(String regionCode, String operatorCode, String query);

    /** 지역의 운영사 목록(요금 기준 설명 포함). 없는 지역이면 TRANSIT_REGION_NOT_FOUND */
    List<OperatorView> listOperators(String regionCode);

    /** 지역 정보(이름·통화·시간대). 없는 지역이면 TRANSIT_REGION_NOT_FOUND */
    RegionView getRegion(String regionCode);
}
