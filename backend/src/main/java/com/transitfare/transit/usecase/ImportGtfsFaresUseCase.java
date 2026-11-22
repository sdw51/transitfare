package com.transitfare.transit.usecase;

import com.transitfare.common.error.BusinessException;
import com.transitfare.transit.domain.FareData;
import com.transitfare.transit.entity.Operator;
import com.transitfare.transit.entity.PairFare;
import com.transitfare.transit.entity.Station;
import com.transitfare.transit.error.TransitErrorCode;
import com.transitfare.transit.provider.GtfsFeed;
import com.transitfare.transit.provider.GtfsFeedReader;
import com.transitfare.transit.repository.OperatorRepository;
import com.transitfare.transit.repository.PairFareRepository;
import com.transitfare.transit.repository.StationRepository;
import com.transitfare.transit.usecase.dto.GtfsImportResult;
import java.io.InputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사용자 행동(관리자): 운영사의 GTFS 파일(쇼와 버스 등)을 올려 정류장과 정류장 간 요금을 한 번에 넣는다 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ImportGtfsFaresUseCase {

    private final OperatorRepository operatorRepository;
    private final StationRepository stationRepository;
    private final PairFareRepository pairFareRepository;
    private final GtfsFeedReader gtfsFeedReader;

    /**
     * GTFS zip에서 정류장(stops.txt)과 구역 간 요금(fare_attributes.txt + fare_rules.txt의 origin_id·destination_id)을
     * station, pair_fare(BASE)로 넣는다. 다시 실행해도 행이 늘지 않고 바뀐 금액만 갱신한다(멱등).
     * 출처는 sourceUrl, 확인 날짜는 오늘.
     *
     * @param operatorCode 가져올 운영사(예: SHOWA_BUS)
     * @param sourceUrl    GTFS를 받은 곳(공식 배포 페이지 URL)
     * @param zip          GTFS zip 내용
     * @return 넣거나 갱신한 정류장 수·요금 수, 가져오지 않은 것의 사유
     * @throws BusinessException TRANSIT_OPERATOR_NOT_FOUND(404, "운영사를 찾을 수 없습니다") — 없는 운영사
     * @throws BusinessException TRANSIT_SOURCE_REQUIRED(400, "출처와 확인 날짜를 입력하세요") — sourceUrl이 비었음
     * @throws BusinessException TRANSIT_GTFS_INVALID(400, "GTFS 파일을 읽을 수 없습니다") — zip이 아니거나 stops.txt 없음
     */
    public GtfsImportResult execute(String operatorCode, String sourceUrl, InputStream zip) {
        Operator operator = operatorRepository.findById(operatorCode)
                .orElseThrow(() -> new BusinessException(TransitErrorCode.TRANSIT_OPERATOR_NOT_FOUND, "operator"));
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new BusinessException(TransitErrorCode.TRANSIT_SOURCE_REQUIRED, "sourceUrl");
        }
        GtfsFeed feed = readFeed(zip);
        LocalDate today = LocalDate.now();
        List<String> skipped = new ArrayList<>();

        // 1. 정류장 → 역 (stop_id → 우리 역)
        Map<String, Station> stationByStopId = importStops(operator, feed.stops(), sourceUrl, today);
        // 2. 구역 간 요금 → 역 쌍 요금
        int fares = importOdFares(operatorCode, feed, stationByStopId, sourceUrl, today, skipped);

        int stations = (int) stationByStopId.values().stream().map(Station::getId).distinct().count();
        log.info("GTFS 가져오기 [{}] 정류장 {}개, 요금 {}개, 건너뜀 {}건", operatorCode, stations, fares, skipped.size());
        return new GtfsImportResult(stations, fares, skipped);
    }

    private GtfsFeed readFeed(InputStream zip) {
        try {
            return gtfsFeedReader.read(zip);
        } catch (IllegalArgumentException e) {
            throw new BusinessException(TransitErrorCode.TRANSIT_GTFS_INVALID, "file");
        }
    }

    /**
     * 정류장을 역으로. 이름이 같은 정류장(같은 이름의 다른 승강장)은 한 역으로 합친다(station은 운영사 안에서 이름이 유일).
     * 이미 있는 역은 그대로 쓰고(관리자가 넣은 한국어 이름 등을 덮지 않음), 없으면 name_ko 없이 새로 만든다.
     */
    private Map<String, Station> importStops(Operator operator, List<Map<String, String>> stops,
                                             String sourceUrl, LocalDate today) {
        Map<String, Station> byName = new HashMap<>();
        Map<String, Station> byStopId = new HashMap<>();
        for (Map<String, String> stop : stops) {
            String stopId = stop.getOrDefault("stop_id", "");
            String name = stop.getOrDefault("stop_name", "");
            if (stopId.isBlank() || name.isBlank()) {
                continue;
            }
            Station station = byName.computeIfAbsent(name, n -> findOrCreateStation(operator, n, sourceUrl, today));
            byStopId.put(stopId, station);
        }
        return byStopId;
    }

    private Station findOrCreateStation(Operator operator, String nameJa, String sourceUrl, LocalDate today) {
        return stationRepository.findByOperatorCodeAndNameJa(operator.getCode(), nameJa)
                .orElseGet(() -> stationRepository.save(Station.builder()
                        .regionCode(operator.getRegionCode())
                        .operatorCode(operator.getCode())
                        .nameJa(nameJa)
                        .sourceUrl(sourceUrl)
                        .verifiedOn(today)
                        .build()));
    }

    /**
     * 구역 간 요금을 역 쌍 요금으로. fare_rules의 origin_id·destination_id는 정류장의 zone_id를 가리킨다.
     * 출발 구역의 역 × 도착 구역의 역 쌍마다 그 fare_id의 가격을 넣는다(양방향 동일이라 한 쌍은 한 번).
     * origin·destination이 없는 규칙(구역 방식: contains_id·route_id만)은 가져오지 않고 사유를 남긴다.
     */
    private int importOdFares(String operatorCode, GtfsFeed feed, Map<String, Station> stationByStopId,
                              String sourceUrl, LocalDate today, List<String> skipped) {
        if (feed.fareAttributes().isEmpty() || feed.fareRules().isEmpty()) {
            skipped.add("요금 파일 없음: fare_attributes.txt·fare_rules.txt가 없어 정류장만 가져왔습니다");
            return 0;
        }
        Map<String, Integer> priceByFareId = prices(feed.fareAttributes().get(), skipped);
        Map<String, Set<Long>> stationIdsByZone = stationIdsByZone(feed.stops(), stationByStopId);

        // 같은 역 쌍이 여러 규칙에 나오면 뒤 규칙이 이긴다
        Map<List<Long>, Integer> priceByPair = new LinkedHashMap<>();
        for (Map<String, String> rule : feed.fareRules().get()) {
            String fareId = rule.getOrDefault("fare_id", "");
            String origin = rule.getOrDefault("origin_id", "");
            String destination = rule.getOrDefault("destination_id", "");
            if (origin.isBlank() || destination.isBlank()) {
                skipped.add("구역 방식 요금 규칙은 가져오지 않습니다: fare_id=" + fareId);
                continue;
            }
            Integer price = priceByFareId.get(fareId);
            if (price == null) {
                skipped.add("fare_attributes.txt에 없는 요금: fare_id=" + fareId);
                continue;
            }
            for (long a : stationIdsByZone.getOrDefault(origin, Set.of())) {
                for (long b : stationIdsByZone.getOrDefault(destination, Set.of())) {
                    if (a != b) {
                        priceByPair.put(List.of(Math.min(a, b), Math.max(a, b)), price);
                    }
                }
            }
        }
        priceByPair.forEach((pair, price) ->
                upsertPairFare(operatorCode, pair.get(0), pair.get(1), price, sourceUrl, today));
        return priceByPair.size();
    }

    /** fare_id → 가격(정수 엔). 숫자가 아니거나 소수면 사유를 남기고 뺀다 */
    private static Map<String, Integer> prices(List<Map<String, String>> fareAttributes, List<String> skipped) {
        Map<String, Integer> prices = new HashMap<>();
        for (Map<String, String> fare : fareAttributes) {
            String fareId = fare.getOrDefault("fare_id", "");
            try {
                prices.put(fareId, new BigDecimal(fare.getOrDefault("price", "")).intValueExact());
            } catch (ArithmeticException | NumberFormatException e) {
                skipped.add("가격을 읽을 수 없는 요금: fare_id=" + fareId);
            }
        }
        return prices;
    }

    /** zone_id → 그 구역에 있는 우리 역 id들 */
    private static Map<String, Set<Long>> stationIdsByZone(List<Map<String, String>> stops,
                                                           Map<String, Station> stationByStopId) {
        Map<String, Set<Long>> result = new HashMap<>();
        for (Map<String, String> stop : stops) {
            String zone = stop.getOrDefault("zone_id", "");
            Station station = stationByStopId.get(stop.getOrDefault("stop_id", ""));
            if (!zone.isBlank() && station != null) {
                result.computeIfAbsent(zone, z -> new TreeSet<>()).add(station.getId());
            }
        }
        return result;
    }

    /** 이미 있는 쌍(방향 무관)이면 금액·출처·확인 날짜를 갱신, 없으면 새로 넣는다 */
    private void upsertPairFare(String operatorCode, long a, long b, int amount, String sourceUrl, LocalDate today) {
        pairFareRepository.findPair(operatorCode, FareData.BASE, a, b).ifPresentOrElse(
                fare -> fare.changeAmount(amount, sourceUrl, today),
                () -> pairFareRepository.save(PairFare.builder()
                        .operatorCode(operatorCode)
                        .stationAId(a)
                        .stationBId(b)
                        .kind(FareData.BASE)
                        .amount(amount)
                        .sourceUrl(sourceUrl)
                        .verifiedOn(today)
                        .build()));
    }
}
