package com.transitfare.transit.contract;

/**
 * 매칭 결과: 우리 데이터의 운영사·노선·역. directions가 이걸로 LegInput을 만든다(좌석은 기본 자유석, 사용자가 바꿈).
 *
 * @param operatorCode  운영사 코드
 * @param lineId        두 역이 모두 속한 노선(노선 데이터가 없는 버스 등은 null)
 * @param fromStationId 승차역 id
 * @param toStationId   하차역 id
 * @param trainClass    열차·노선 이름에 "特急"이 있으면 EXPRESS
 */
public record MatchedLeg(String operatorCode, Long lineId, long fromStationId, long toStationId,
                         TrainClass trainClass) {
}
