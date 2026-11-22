package com.transitfare.transit.domain;

import com.transitfare.transit.contract.FareStatus;
import com.transitfare.transit.contract.LegInput;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 회사 간 환승 할인(설계 7-1 환승 할인 규칙표). 직전에 탄 운영사와 다른 운영사로 갈아탔고
 * from→to 할인이 등록돼 있으면 갈아탄 구간 운임에서 뺀다. 0원 아래로는 내리지 않는다.
 */
class TransferDiscountApplier {

    private final FareData data;

    TransferDiscountApplier(FareData data) {
        this.data = data;
    }

    /**
     * @param legs    구간 목록
     * @param charges 방식별 계산 결과(legs와 같은 순서)
     * @return 할인을 적용한 새 목록
     */
    List<LegCharge> apply(List<LegInput> legs, List<LegCharge> charges) {
        List<LegCharge> result = new ArrayList<>(charges);
        String previousOperator = null;  // 직전에 탄 운영사
        for (int i = 0; i < legs.size(); i++) {
            LegInput leg = legs.get(i);
            switch (leg.mode()) {
                case WALK -> { }                        // 도보로 갈아타도 환승이므로 직전 운영사를 그대로 기억
                case MANUAL -> previousOperator = null; // 직접 입력 구간이 끼면 환승으로 보지 않는다
                case TRANSIT -> {
                    String operator = leg.operatorCode();
                    LegCharge charge = result.get(i);
                    if (isTransfer(previousOperator, operator) && charge.status() == FareStatus.OK) {
                        Optional<Integer> discount = data.transferDiscount(previousOperator, operator);
                        if (discount.isPresent()) {
                            result.set(i, charge.discounted(discount.get()));
                        }
                    }
                    previousOperator = operator;
                }
            }
        }
        return result;
    }

    /** 직전 운영사가 있고 지금 운영사와 다를 때만 환승 */
    private static boolean isTransfer(String previous, String current) {
        return previous != null && current != null && !previous.equals(current);
    }
}
