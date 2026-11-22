package com.transitfare.transit.domain;

import com.transitfare.transit.contract.FareStatus;
import com.transitfare.transit.contract.LegFare;
import com.transitfare.transit.contract.Passengers;
import java.util.List;

/**
 * 계산 중인 구간 요금(어른 1명 기준). 계산기들이 만들고 FareEngine이 마지막에 LegFare로 바꾼다.
 * UNKNOWN이면 금액은 모두 0이고 note에 빠진 항목을 적는다.
 */
record LegCharge(FareStatus status, int base, int express, int reserved, String note, Integer groupId) {

    /** 도보: 0원 */
    static LegCharge free() {
        return new LegCharge(FareStatus.OK, 0, 0, 0, null, null);
    }

    static LegCharge ok(int base, int express, int reserved, String note) {
        return new LegCharge(FareStatus.OK, base, express, reserved, note, null);
    }

    /** 직접 입력 금액 */
    static LegCharge manual(int amount) {
        return new LegCharge(FareStatus.MANUAL, amount, 0, 0, null, null);
    }

    static LegCharge unknown(String note) {
        return new LegCharge(FareStatus.UNKNOWN, 0, 0, 0, note, null);
    }

    /** 빠진 요금 항목들로 미확인. 예: "요금 데이터 없음: 운임, 특급요금" */
    static LegCharge missing(List<String> items) {
        return unknown("요금 데이터 없음: " + String.join(", ", items.stream().distinct().toList()));
    }

    /** 거리제 묶음 번호를 붙인다 */
    LegCharge inGroup(Integer groupId) {
        return new LegCharge(status, base, express, reserved, note, groupId);
    }

    /** 환승 할인: 운임에서 빼되 0원 아래로는 내리지 않고, note에 할인 금액을 남긴다 */
    LegCharge discounted(int discount) {
        String add = "환승 할인 -" + discount;
        return new LegCharge(status, Math.max(0, base - discount), express, reserved,
                note == null ? add : note + " · " + add, groupId);
    }

    /** 공개 결과로 변환. partyTotal = (base + express + reserved) × 어른 수 */
    LegFare toLegFare(int legIndex, String operatorCode, Passengers passengers) {
        int partyTotal = (base + express + reserved) * passengers.adults();
        return new LegFare(legIndex, status, base, express, reserved, partyTotal, operatorCode, note, groupId);
    }
}
