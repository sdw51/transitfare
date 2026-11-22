package com.transitfare.transit.controller;

import com.transitfare.transit.TransitFacade;
import com.transitfare.transit.contract.OperatorView;
import com.transitfare.transit.contract.RegionView;
import com.transitfare.transit.contract.StationView;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 지역·운영사·역 조회 API (읽기 전용 — 아무것도 저장하지 않는다).
 *
 * <p>부르는 곳은 프론트뿐이다. 사용자가 일정 입력 화면(Task 9 "역 직접 선택")에서 운영사를 고르고
 * 역 이름을 타이핑하는 동안 화면이 뒤에서 이 API들을 불러 드롭다운·자동완성 목록을 채운다.
 * 사용자가 고른 역은 id 값만 itinerary 모듈의 여행·일정 API(/api/v1/trips…)로 저장된다.
 *
 * <p>다른 모듈(itinerary, directions)은 이 HTTP API가 아니라 {@link TransitFacade}를 자바로 직접 호출한다.
 * 이 컨트롤러도 같은 입구를 쓰도록 퍼사드를 거친다.
 */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class StationController {

    private final TransitFacade transitFacade;

    /**
     * 지역 정보. GET /api/v1/regions/{code}
     *
     * <p>언제: 여행 화면이 지역 이름·통화(예: JPY)·시간대를 보여줄 때, 금액 단위를 정할 때.
     * {@link TransitFacade#getRegion}: 지역 코드로 지역 이름·통화·시간대를 찾는다.
     *
     * @param code 지역 코드(예: JP-FUKUOKA)
     * @return 지역 정보
     * @throws com.transitfare.common.error.BusinessException TRANSIT_REGION_NOT_FOUND(404) — 없는 지역
     */
    @GetMapping("/regions/{code}")
    public RegionView getRegion(@PathVariable String code) {
        return transitFacade.getRegion(code);
    }

    /**
     * 지역의 운영사 목록. GET /api/v1/operators?region=JP-FUKUOKA
     *
     * <p>언제: 일정 입력 화면의 운영사 드롭다운을 채울 때. 운영사마다 요금 기준 설명
     * (fareBasisNote, 예: "현금 요금 기준")을 같이 받아 화면에 표시한다.
     * {@link TransitFacade#listOperators}: 운영사를 코드 순으로, 요금 기준 설명과 함께 돌려준다.
     *
     * @param region 지역 코드(필수, 없으면 400 INVALID_INPUT)
     * @return 운영사 목록
     * @throws com.transitfare.common.error.BusinessException TRANSIT_REGION_NOT_FOUND(404) — 없는 지역
     */
    @GetMapping("/operators")
    public List<OperatorView> listOperators(@RequestParam("region") String region) {
        return transitFacade.listOperators(region);
    }

    /**
     * 역 검색. GET /api/v1/stations?region=JP-FUKUOKA&operator=FUKUOKA_SUBWAY&q=博多
     *
     * <p>언제: 사용자가 역 검색칸에 "博多"·"하카타" 같은 이름을 타이핑할 때 자동완성 목록을 채운다.
     * 운영사를 먼저 골랐으면 operator로 그 운영사 역만 찾는다. 응답의 id를 화면이 구간(승차역·하차역)으로 쓴다.
     * {@link TransitFacade#searchStations}: 이름(일본어·한국어)·별칭 부분 일치로 역을 찾고 노선을 붙인다. 최대 30개.
     *
     * @param region   지역 코드(필수, 없으면 400 INVALID_INPUT)
     * @param operator 운영사 코드(선택, 없으면 지역 전체)
     * @param q        검색어(선택, 없으면 앞에서부터 30개)
     * @return 역 목록(id 순)
     * @throws com.transitfare.common.error.BusinessException TRANSIT_REGION_NOT_FOUND(404) — 없는 지역
     */
    @GetMapping("/stations")
    public List<StationView> searchStations(@RequestParam("region") String region,
                                            @RequestParam(value = "operator", required = false) String operator,
                                            @RequestParam(value = "q", required = false) String q) {
        return transitFacade.searchStations(region, operator, q);
    }
}
