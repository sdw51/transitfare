package com.transitfare.transit.usecase.dto;

import java.util.List;

/**
 * GTFS 가져오기 결과(관리자 화면에 그대로 보여준다).
 *
 * @param stationsUpserted 넣거나 이미 있던 역 수(같은 이름 승강장은 한 역)
 * @param faresUpserted    넣거나 금액을 갱신한 역 쌍 요금 수
 * @param skipped          가져오지 않은 것의 사유(구역 방식 규칙, 요금 파일 없음 등)
 */
public record GtfsImportResult(int stationsUpserted, int faresUpserted, List<String> skipped) {
}
