package com.transitfare.transit.contract;

import java.util.List;

/** 역 검색 결과. 같은 실제 역도 운영사별로 따로 나온다 */
public record StationView(long id, String operatorCode, String nameJa, String nameKo, List<LineRef> lines) {
}
