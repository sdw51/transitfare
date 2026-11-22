package com.transitfare.transit.contract;

/**
 * 구간 하나의 요금. 금액은 어른 1명 기준(base 운임, express 특급요금, reserved 지정석 요금).
 * partyTotal = (base + express + reserved) × adults. UNKNOWN이면 금액은 모두 0이고 note에 빠진 항목.
 * 연속 거리제 묶음은 첫 구간에 금액, 이어지는 구간은 0 + note "앞 구간과 합산". 같은 묶음은 같은 groupId(묶음 아니면 null).
 */
public record LegFare(int legIndex, FareStatus status,
                      int base, int express, int reserved,
                      int partyTotal, String operatorCode, String note, Integer groupId) {
}
