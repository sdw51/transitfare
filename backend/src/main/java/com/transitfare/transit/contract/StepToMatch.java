package com.transitfare.transit.contract;

/**
 * 매칭할 경로 단계(Google 경로 후보의 탑승 단계 하나 — 탈것 하나로 갈아타지 않고 가는 구간). directions가 Google 응답에서 채운다.
 *
 * @param agencyName        운영사 이름(예: "JR九州") — operator_alias와 정확히 비교
 * @param lineName          노선 이름(예: "鹿児島本線", 없을 수 있음)
 * @param trainName         열차 이름(예: "特急かもめ", 없을 수 있음)
 * @param departureStopName 승차 정류장 이름
 * @param arrivalStopName   하차 정류장 이름
 */
public record StepToMatch(String agencyName, String lineName, String trainName,
                          String departureStopName, String arrivalStopName) {
}
