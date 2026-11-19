package com.transitfare.transit.domain;

import com.transitfare.transit.contract.LegInput;

/** 직접 입력 구간(설계 7-1에 없는 운영사·데이터 밖 구간을 사용자가 금액으로 입력). 상태는 MANUAL */
class ManualFareCalculator {

    /** 입력한 금액(어른 1명)을 운임으로. 금액이 없으면 UNKNOWN(입력 검증은 유스케이스 몫) */
    LegCharge calculate(LegInput leg) {
        return leg.manualAmount() == null
                ? LegCharge.unknown("직접 입력 금액 없음")
                : LegCharge.manual(leg.manualAmount());
    }
}
