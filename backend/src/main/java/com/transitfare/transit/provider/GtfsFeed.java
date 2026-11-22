package com.transitfare.transit.provider;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * GTFS zip을 표로 읽은 결과. 파일마다 행 목록(열 이름 → 값). 없는 파일은 empty.
 * 필요한 파일만 담는다: stops.txt(필수), fare_attributes.txt, fare_rules.txt.
 */
public record GtfsFeed(List<Map<String, String>> stops,
                       Optional<List<Map<String, String>>> fareAttributes,
                       Optional<List<Map<String, String>>> fareRules) {
}
