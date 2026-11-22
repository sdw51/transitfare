package com.transitfare.transit.usecase;

import com.transitfare.common.error.BusinessException;
import com.transitfare.transit.contract.FareStatus;
import com.transitfare.transit.contract.LegFare;
import com.transitfare.transit.contract.LegInput;
import com.transitfare.transit.contract.Passengers;
import com.transitfare.transit.contract.PricingResult;
import com.transitfare.transit.domain.FareEngine;
import com.transitfare.transit.error.TransitErrorCode;
import com.transitfare.transit.repository.OperatorRepository;
import com.transitfare.transit.repository.RegionRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 행동: 구간들의 요금을 본다
 * (일정 입력 중 요금 미리보기, 여행 요약(itinerary), 경로 후보 요금(directions)이 모두 이걸로 계산한다).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PriceLegsUseCase {

    private final RegionRepository regionRepository;
    private final OperatorRepository operatorRepository;
    private final FareEngine fareEngine;

    /**
     * 입력을 확인하고 요금 계산 엔진으로 구간별 요금과 일행 총액을 구한다.
     * 운영사를 모르거나 요금 데이터가 없는 구간은 에러가 아니라 UNKNOWN(hasUnknown=true)으로 돌려준다.
     *
     * @param regionCode 지역 코드
     * @param legs       구간 목록(순서대로)
     * @param passengers 인원(어른 1명 이상)
     * @return 구간별 요금, 일행 총액(확인된 금액만), 미확인 포함 여부
     * @throws BusinessException TRANSIT_INVALID_PASSENGERS(400, "인원을 1명 이상 입력하세요") — 어른 0명 이하
     * @throws BusinessException TRANSIT_REGION_NOT_FOUND(404, "지역을 찾을 수 없습니다") — 없는 지역
     * @throws BusinessException TRANSIT_OPERATOR_REGION_MISMATCH(400, "다른 지역의 운영사가 섞여 있습니다") — 다른 지역 운영사가 섞임
     */
    public PricingResult execute(String regionCode, List<LegInput> legs, Passengers passengers) {
        if (passengers == null || passengers.adults() < 1) {
            throw new BusinessException(TransitErrorCode.TRANSIT_INVALID_PASSENGERS, "passengers.adults");
        }
        if (!regionRepository.existsById(regionCode)) {
            throw new BusinessException(TransitErrorCode.TRANSIT_REGION_NOT_FOUND, "regionCode");
        }
        checkOperatorsInRegion(regionCode, legs);

        List<LegFare> fares = fareEngine.price(legs, passengers);
        int partyTotal = fares.stream().mapToInt(LegFare::partyTotal).sum();
        boolean hasUnknown = fares.stream().anyMatch(f -> f.status() == FareStatus.UNKNOWN);
        return new PricingResult(fares, partyTotal, hasUnknown);
    }

    /**
     * 구간의 운영사가 모두 이 지역 것인지 확인한다.
     * 운영사 코드가 없거나 DB에 없는 운영사는 여기서 막지 않는다(엔진이 "운영사 미확인"으로 처리 —
     * 경로 후보에서 매칭에 실패한 구간도 후보를 유지해야 하므로, 계획 리뷰 포커스 5).
     */
    private void checkOperatorsInRegion(String regionCode, List<LegInput> legs) {
        for (int i = 0; i < legs.size(); i++) {
            String operatorCode = legs.get(i).operatorCode();
            if (operatorCode == null) {
                continue;
            }
            boolean otherRegion = operatorRepository.findById(operatorCode)
                    .map(o -> !o.getRegionCode().equals(regionCode))
                    .orElse(false);
            if (otherRegion) {
                throw new BusinessException(TransitErrorCode.TRANSIT_OPERATOR_REGION_MISMATCH,
                        "legs[" + i + "].operatorCode");
            }
        }
    }
}
