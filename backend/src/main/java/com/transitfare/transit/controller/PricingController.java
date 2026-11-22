package com.transitfare.transit.controller;

import com.transitfare.transit.TransitFacade;
import com.transitfare.transit.contract.PricingResult;
import com.transitfare.transit.controller.dto.PricingRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 구간 요금 계산 API (계산만 하고 아무것도 저장하지 않는다).
 *
 * <p>부르는 곳은 프론트뿐이다. 사용자가 일정 입력 화면(Task 9 "역 직접 선택")에서 운영사·역·특급/보통·지정석/자유석을
 * 고르면, 저장하기 전에 화면이 이 API로 요금 미리보기를 보여준다. 저장은 itinerary 모듈의 일정 API가 한다.
 *
 * <p>여행 요약(itinerary)과 경로 후보(directions)는 이 HTTP API가 아니라 {@link TransitFacade#priceLegs}를 자바로 직접 부른다.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PricingController {

    private final TransitFacade transitFacade;

    /**
     * 구간 요금 계산. POST /api/v1/pricing
     *
     * <p>언제: 사용자가 구간을 하나 고르거나 바꿀 때마다(탈것 여러 개면 전체 목록을 보내서 거리제 합산·환승 할인까지 반영).
     * {@link TransitFacade#priceLegs}: 인원·지역·운영사 지역을 확인하고 요금 계산 엔진으로 구간별 요금과 일행 총액을 구한다.
     * 요금 데이터가 없는 구간은 에러가 아니라 status UNKNOWN + hasUnknown=true("미확인 구간 포함" 경고용).
     *
     * @param request 지역, 인원, 구간 목록. enum 값은 코드 문자열("EXPRESS")이나 응답에서 받은 객체 둘 다 된다
     * @return 구간별 요금(어른 1명 기준 금액 + 일행 금액), 일행 총액, 미확인 포함 여부
     * @throws com.transitfare.common.error.BusinessException
     *         TRANSIT_INVALID_PASSENGERS(400, "인원을 1명 이상 입력하세요"),
     *         TRANSIT_REGION_NOT_FOUND(404, "지역을 찾을 수 없습니다"),
     *         TRANSIT_OPERATOR_REGION_MISMATCH(400, "다른 지역의 운영사가 섞여 있습니다").
     *         필수값이 빠졌거나 모르는 enum 코드면 INVALID_INPUT(400, "입력값을 확인하세요")
     */
    @PostMapping("/pricing")
    public PricingResult price(@Valid @RequestBody PricingRequest request) {
        return transitFacade.priceLegs(request.regionCode(), request.legs(), request.passengers());
    }
}
