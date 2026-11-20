package com.transitfare.transit;

import com.transitfare.transit.contract.LegInput;
import com.transitfare.transit.contract.MatchedLeg;
import com.transitfare.transit.contract.OperatorView;
import com.transitfare.transit.contract.Passengers;
import com.transitfare.transit.contract.PricingResult;
import com.transitfare.transit.contract.RegionView;
import com.transitfare.transit.contract.StationView;
import com.transitfare.transit.contract.StepToMatch;
import com.transitfare.transit.usecase.GetRegionUseCase;
import com.transitfare.transit.usecase.ListOperatorsUseCase;
import com.transitfare.transit.usecase.MatchStepUseCase;
import com.transitfare.transit.usecase.PriceLegsUseCase;
import com.transitfare.transit.usecase.SearchStationsUseCase;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** TransitFacade 구현. 메서드마다 사용자 행동 하나에 해당하는 유스케이스로 넘긴다. */
@Service
@RequiredArgsConstructor
public class TransitFacadeImpl implements TransitFacade {

    private final PriceLegsUseCase priceLegsUseCase;
    private final SearchStationsUseCase searchStationsUseCase;
    private final ListOperatorsUseCase listOperatorsUseCase;
    private final GetRegionUseCase getRegionUseCase;
    private final MatchStepUseCase matchStepUseCase;

    /**
     * {@link PriceLegsUseCase}: 인원·지역·운영사 지역을 확인하고, 요금 계산 엔진으로 구간별 요금을 구해 일행 총액을 더한다
     */
    @Override
    public PricingResult priceLegs(String regionCode, List<LegInput> legs, Passengers passengers) {
        return priceLegsUseCase.execute(regionCode, legs, passengers);
    }

    /** {@link SearchStationsUseCase}: 이름·별칭 부분 일치로 역을 찾고 각 역의 노선을 붙인다 */
    @Override
    public List<StationView> searchStations(String regionCode, String operatorCode, String query) {
        return searchStationsUseCase.execute(regionCode, operatorCode, query);
    }

    /** {@link ListOperatorsUseCase}: 지역 확인 후 운영사를 코드 순으로 돌려준다 */
    @Override
    public List<OperatorView> listOperators(String regionCode) {
        return listOperatorsUseCase.execute(regionCode);
    }

    /** {@link GetRegionUseCase}: 지역 코드로 지역 정보를 찾는다 */
    @Override
    public RegionView getRegion(String regionCode) {
        return getRegionUseCase.execute(regionCode);
    }

    /**
     * {@link MatchStepUseCase}: 운영사 별칭 정확 일치 → 역 이름·별칭 일치(안 되면 정규화 후) → 두 역의 공통 노선 →
     * 이름의 "特急"으로 열차 종별을 정해 매칭 결과를 만든다
     */
    @Override
    public Optional<MatchedLeg> matchStep(String regionCode, StepToMatch step) {
        return matchStepUseCase.execute(regionCode, step);
    }
}
