package com.transitfare.transit.usecase;

import com.transitfare.common.error.BusinessException;
import com.transitfare.transit.contract.OperatorView;
import com.transitfare.transit.error.TransitErrorCode;
import com.transitfare.transit.repository.OperatorRepository;
import com.transitfare.transit.repository.RegionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사용자 행동: 일정 입력 때 지역의 운영사 목록을 고른다 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ListOperatorsUseCase {

    private final RegionRepository regionRepository;
    private final OperatorRepository operatorRepository;

    /**
     * 지역의 운영사 목록을 코드 순으로 돌려준다.
     *
     * @param regionCode 지역 코드
     * @return 운영사 목록(요금 기준 설명 포함)
     * @throws BusinessException TRANSIT_REGION_NOT_FOUND(404) — 없는 지역
     */
    public List<OperatorView> execute(String regionCode) {
        if (!regionRepository.existsById(regionCode)) {
            throw new BusinessException(TransitErrorCode.TRANSIT_REGION_NOT_FOUND, "regionCode");
        }
        return operatorRepository.findByRegionCodeOrderByCode(regionCode).stream()
                .map(o -> new OperatorView(o.getCode(), o.getNameJa(), o.getNameKo(), o.getFareBasisNote()))
                .toList();
    }
}
