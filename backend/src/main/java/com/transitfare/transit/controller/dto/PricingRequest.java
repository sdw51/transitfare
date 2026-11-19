package com.transitfare.transit.controller.dto;

import com.transitfare.transit.contract.LegInput;
import com.transitfare.transit.contract.Passengers;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

/**
 * 구간 요금 계산 요청(POST /api/v1/pricing 본문). transit 컨트롤러 전용이라 contract가 아니다
 * (다른 모듈은 HTTP가 아니라 TransitFacade.priceLegs를 부른다). 프론트 api/types.ts는 같은 필드로 맞춘다.
 * 값이 있는지만 여기서 검사하고(빠지면 400 INVALID_INPUT), 인원 1명 이상·지역 존재는 유스케이스가 검사한다.
 *
 * @param regionCode 지역 코드(예: JP-FUKUOKA)
 * @param passengers 인원(어른 수)
 * @param legs       구간 목록(순서대로, 1개 이상)
 */
public record PricingRequest(@NotBlank String regionCode,
                             @NotNull Passengers passengers,
                             @NotEmpty List<LegInput> legs) {
}
