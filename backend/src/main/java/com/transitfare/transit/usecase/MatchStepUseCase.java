package com.transitfare.transit.usecase;

import com.transitfare.transit.contract.MatchedLeg;
import com.transitfare.transit.contract.StepToMatch;
import com.transitfare.transit.contract.TrainClass;
import com.transitfare.transit.domain.StationNameNormalizer;
import com.transitfare.transit.entity.Line;
import com.transitfare.transit.entity.Station;
import com.transitfare.transit.entity.StationAlias;
import com.transitfare.transit.repository.LineRepository;
import com.transitfare.transit.repository.LineStationRepository;
import com.transitfare.transit.repository.OperatorAliasRepository;
import com.transitfare.transit.repository.StationAliasRepository;
import com.transitfare.transit.repository.StationRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 사용자 행동: 경로 후보에서 탈 경로를 고를 때, Google 경로의 탑승 단계를 우리 역·노선으로 바꾼다
 * (directions 모듈이 후보의 단계마다 부른다. 매칭에 실패하면 그 구간만 요금 미확인이 된다).
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MatchStepUseCase {

    /** 열차·노선 이름에 이 글자가 있으면 특급(특급요금이 붙는 열차) */
    static final String EXPRESS_MARK = "特急";

    private final OperatorAliasRepository operatorAliasRepository;
    private final StationRepository stationRepository;
    private final StationAliasRepository stationAliasRepository;
    private final LineStationRepository lineStationRepository;
    private final LineRepository lineRepository;

    /**
     * 경로 단계 하나를 우리 운영사·노선·역으로 매칭한다(계획 Task 10-A 규칙). 에러를 던지지 않는다.
     *
     * @param regionCode 지역 코드(이 지역 운영사만 찾는다)
     * @param step       Google 경로의 탑승 단계(운영사·노선·열차·정류장 이름)
     * @return 매칭 결과. 운영사나 역을 못 찾으면 빈 값(directions가 UNKNOWN 구간 + 매칭 실패 기록으로 처리)
     */
    public Optional<MatchedLeg> execute(String regionCode, StepToMatch step) {
        // 같은 별칭의 운영사가 여럿이면(예: "西日本鉄道" = 전철·버스) 두 역이 모두 있는 첫 운영사로 정한다
        for (String operatorCode : operatorAliasRepository.findOperatorCodes(regionCode, step.agencyName())) {
            StationNames stations = loadStationNames(operatorCode);
            Optional<Station> from = stations.find(step.departureStopName());
            Optional<Station> to = stations.find(step.arrivalStopName());
            if (from.isPresent() && to.isPresent()) {
                Long lineId = commonLine(from.get().getId(), to.get().getId(), step.lineName()).orElse(null);
                return Optional.of(new MatchedLeg(operatorCode, lineId, from.get().getId(), to.get().getId(),
                        trainClassOf(step)));
            }
        }
        return Optional.empty();
    }

    /** 운영사의 역과 각 역의 이름들(name_ja + 별칭)을 한 번에 읽는다 */
    private StationNames loadStationNames(String operatorCode) {
        List<Station> stations = stationRepository.findByOperatorCodeOrderById(operatorCode);
        Map<Long, List<String>> aliases = stationAliasRepository
                .findByStationIdIn(stations.stream().map(Station::getId).toList()).stream()
                .collect(Collectors.groupingBy(StationAlias::getStationId,
                        Collectors.mapping(StationAlias::getAlias, Collectors.toList())));
        return new StationNames(stations, aliases);
    }

    /**
     * 두 역이 모두 속한 노선. 하나면 그것, 여럿이면 이름이 lineName과 겹치는 것(서로 포함), 그래도 여럿이면 id가 가장 작은 것.
     * 공통 노선이 없으면(노선 데이터가 없는 버스, 노선을 넘어 달리는 직통 열차 등) 빈 값.
     */
    private Optional<Long> commonLine(long fromId, long toId, String lineName) {
        Map<Long, Set<Long>> linesByStation = lineStationRepository.findByIdStationIdIn(List.of(fromId, toId)).stream()
                .collect(Collectors.groupingBy(ls -> ls.getId().getStationId(),
                        Collectors.mapping(ls -> ls.getId().getLineId(), Collectors.toSet())));
        Set<Long> common = new TreeSet<>(linesByStation.getOrDefault(fromId, Set.of()));
        common.retainAll(linesByStation.getOrDefault(toId, Set.of()));
        if (common.size() <= 1) {
            return common.stream().findFirst();
        }
        List<Line> lines = lineRepository.findAllById(common).stream()
                .sorted(Comparator.comparing(Line::getId))
                .toList();
        return lines.stream()
                .filter(line -> lineName != null
                        && (line.getNameJa().contains(lineName) || lineName.contains(line.getNameJa())))
                .findFirst()
                .or(() -> lines.stream().findFirst())
                .map(Line::getId);
    }

    /** 열차 이름이나 노선 이름에 "特急"이 있으면 EXPRESS. 좌석은 여기서 정하지 않는다(기본 자유석, 사용자가 바꿈) */
    private static TrainClass trainClassOf(StepToMatch step) {
        boolean express = contains(step.trainName(), EXPRESS_MARK) || contains(step.lineName(), EXPRESS_MARK);
        return express ? TrainClass.EXPRESS : TrainClass.LOCAL;
    }

    private static boolean contains(String text, String mark) {
        return text != null && text.contains(mark);
    }

    /** 한 운영사의 역 이름표: 역마다 name_ja와 별칭들 */
    private record StationNames(List<Station> stations, Map<Long, List<String>> aliases) {

        /** 이름으로 역 찾기: ① name_ja·별칭 정확 일치 → ② 정규화(駅·공백·전각 제거) 후 일치 */
        Optional<Station> find(String stopName) {
            if (stopName == null || stopName.isBlank()) {
                return Optional.empty();
            }
            return stations.stream().filter(s -> namesOf(s).contains(stopName)).findFirst()
                    .or(() -> {
                        String normalized = StationNameNormalizer.normalize(stopName);
                        return stations.stream()
                                .filter(s -> namesOf(s).stream()
                                        .map(StationNameNormalizer::normalize)
                                        .anyMatch(normalized::equals))
                                .findFirst();
                    });
        }

        private List<String> namesOf(Station station) {
            List<String> names = new ArrayList<>(aliases.getOrDefault(station.getId(), List.of()));
            names.add(station.getNameJa());
            return names;
        }
    }
}
