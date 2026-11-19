package com.transitfare.transit;

import com.transitfare.transit.contract.OperatorView;
import com.transitfare.transit.contract.RegionView;
import com.transitfare.transit.contract.StationView;
import com.transitfare.transit.usecase.GetRegionUseCase;
import com.transitfare.transit.usecase.ListOperatorsUseCase;
import com.transitfare.transit.usecase.SearchStationsUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** TransitFacade 구현. 메서드마다 사용자 행동 하나에 해당하는 유스케이스로 넘긴다. */
@Service
@RequiredArgsConstructor
public class TransitFacadeImpl implements TransitFacade {

    private final SearchStationsUseCase searchStationsUseCase;
    private final ListOperatorsUseCase listOperatorsUseCase;
    private final GetRegionUseCase getRegionUseCase;

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
}
