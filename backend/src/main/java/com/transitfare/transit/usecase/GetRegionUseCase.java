package com.transitfare.transit.usecase;

import com.transitfare.common.error.BusinessException;
import com.transitfare.transit.contract.RegionView;
import com.transitfare.transit.entity.Region;
import com.transitfare.transit.error.TransitErrorCode;
import com.transitfare.transit.repository.RegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사용자 행동: 여행 지역 정보를 본다 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GetRegionUseCase {

    private final RegionRepository regionRepository;

    /**
     * 지역 코드로 지역 정보를 찾는다.
     *
     * @param regionCode 지역 코드(예: JP-FUKUOKA)
     * @return 지역 이름·통화·시간대
     * @throws BusinessException TRANSIT_REGION_NOT_FOUND(404) — 없는 지역
     */
    public RegionView execute(String regionCode) {
        Region region = regionRepository.findById(regionCode)
                .orElseThrow(() -> new BusinessException(TransitErrorCode.TRANSIT_REGION_NOT_FOUND, "regionCode"));
        return new RegionView(region.getCode(), region.getNameKo(), region.getCurrency(), region.getTimezone());
    }
}
