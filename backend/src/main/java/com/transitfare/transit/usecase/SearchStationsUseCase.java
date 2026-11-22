package com.transitfare.transit.usecase;

import com.transitfare.common.error.BusinessException;
import com.transitfare.transit.contract.LineRef;
import com.transitfare.transit.contract.StationView;
import com.transitfare.transit.entity.Line;
import com.transitfare.transit.entity.LineStation;
import com.transitfare.transit.entity.Station;
import com.transitfare.transit.error.TransitErrorCode;
import com.transitfare.transit.repository.LineRepository;
import com.transitfare.transit.repository.LineStationRepository;
import com.transitfare.transit.repository.RegionRepository;
import com.transitfare.transit.repository.StationRepository;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사용자 행동: 일정 입력 때 역·정류장을 이름으로 검색한다 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchStationsUseCase {

    /** 한 번에 돌려주는 최대 역 수(계획: 최대 30) */
    static final int MAX_RESULTS = 30;

    private final RegionRepository regionRepository;
    private final StationRepository stationRepository;
    private final LineStationRepository lineStationRepository;
    private final LineRepository lineRepository;

    /**
     * 일본어 이름·한국어 이름·별칭 부분 일치로 역을 찾고, 각 역이 속한 노선을 붙여 돌려준다.
     *
     * @param regionCode   지역 코드(필수)
     * @param operatorCode 운영사 코드(null이면 지역 전체)
     * @param query        검색어(null·빈 문자열이면 조건 없이 앞에서부터 30개)
     * @return 최대 30개 역, id 순
     * @throws BusinessException TRANSIT_REGION_NOT_FOUND(404) — 없는 지역
     */
    public List<StationView> execute(String regionCode, String operatorCode, String query) {
        if (!regionRepository.existsById(regionCode)) {
            throw new BusinessException(TransitErrorCode.TRANSIT_REGION_NOT_FOUND, "regionCode");
        }
        String pattern = "%" + (query == null ? "" : query.strip().toLowerCase(Locale.ROOT)) + "%";
        List<Station> stations = stationRepository.search(
                regionCode, blankToNull(operatorCode), pattern, Limit.of(MAX_RESULTS));
        Map<Long, List<LineRef>> linesByStation = linesOf(stations);
        return stations.stream()
                .map(s -> new StationView(s.getId(), s.getOperatorCode(), s.getNameJa(), s.getNameKo(),
                        linesByStation.getOrDefault(s.getId(), List.of())))
                .toList();
    }

    /** 역 id → 그 역이 속한 노선 목록. 쿼리 두 번으로 한꺼번에 가져온다(역마다 조회하지 않게) */
    private Map<Long, List<LineRef>> linesOf(List<Station> stations) {
        List<Long> stationIds = stations.stream().map(Station::getId).toList();
        List<LineStation> links = lineStationRepository.findByIdStationIdIn(stationIds);
        Map<Long, Line> lines = lineRepository
                .findAllById(links.stream().map(l -> l.getId().getLineId()).distinct().toList())
                .stream().collect(Collectors.toMap(Line::getId, l -> l));
        return links.stream().collect(Collectors.groupingBy(
                l -> l.getId().getStationId(),
                Collectors.mapping(
                        l -> new LineRef(l.getId().getLineId(), lines.get(l.getId().getLineId()).getNameKo()),
                        Collectors.toList())));
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
