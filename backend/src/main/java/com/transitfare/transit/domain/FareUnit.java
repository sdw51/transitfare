package com.transitfare.transit.domain;

import com.transitfare.transit.contract.LegInput;
import com.transitfare.transit.contract.LegMode;
import java.util.ArrayList;
import java.util.List;

/**
 * 계산 단위: 요금을 한 번에 계산하는 이어진 구간들.
 * 거리제 묶음은 같은 운영사 구간 여러 개(사이 도보 포함), 그 밖에는 구간 하나.
 *
 * @param legs    단위 안 구간들(입력 순서 그대로)
 * @param rule    운영사 요금 규칙. 도보·직접 입력이거나 운영사를 못 찾으면 null
 * @param groupId 거리제 묶음 번호(탄 구간이 2개 이상일 때만, 아니면 null)
 */
record FareUnit(List<LegInput> legs, OperatorRule rule, Integer groupId) {

    LegInput first() {
        return legs.get(0);
    }

    /** 구간 목록을 계산 단위로 자른다. 단위들을 이어 붙이면 원래 목록과 같다 */
    static List<FareUnit> split(List<LegInput> legs, FareData data) {
        List<FareUnit> units = new ArrayList<>();
        int nextGroupId = 1;
        int i = 0;
        while (i < legs.size()) {
            LegInput leg = legs.get(i);
            OperatorRule rule = ruleOf(leg, data);
            if (rule == null || rule.ruleType() != FareRuleType.DISTANCE_BAND) {
                units.add(new FareUnit(List.of(leg), rule, null));
                i++;
                continue;
            }
            int end = distanceGroupEnd(legs, i);
            List<LegInput> group = legs.subList(i, end + 1);
            boolean manyRides = group.stream().filter(l -> l.mode() == LegMode.TRANSIT).count() > 1;
            units.add(new FareUnit(group, rule, manyRides ? nextGroupId++ : null));
            i = end + 1;
        }
        return units;
    }

    /** TRANSIT 구간의 운영사 규칙. 도보·직접 입력이거나 운영사를 못 찾으면 null */
    private static OperatorRule ruleOf(LegInput leg, FareData data) {
        if (leg.mode() != LegMode.TRANSIT || leg.operatorCode() == null) {
            return null;
        }
        return data.operator(leg.operatorCode()).orElse(null);
    }

    /**
     * start(거리제 구간)부터 같은 운영사 구간이 이어지는 마지막 위치(설계 7-1: 지하철 노선 환승·JR 노선 갈아타기는 거리 합산).
     * 사이의 도보는 묶음에 넣고, 다른 운영사나 직접 입력이 나오면 멈춘다. 끝에 붙은 도보는 넣지 않는다.
     */
    private static int distanceGroupEnd(List<LegInput> legs, int start) {
        String operator = legs.get(start).operatorCode();
        int end = start;
        for (int j = start + 1; j < legs.size(); j++) {
            LegInput next = legs.get(j);
            if (next.mode() == LegMode.WALK) {
                continue;                       // 도보는 일단 지나가고, 뒤에 같은 운영사가 나오면 포함된다
            }
            if (next.mode() == LegMode.TRANSIT && operator.equals(next.operatorCode())) {
                end = j;                        // 같은 운영사 → 묶음을 여기까지 늘린다
                continue;
            }
            break;                              // 다른 운영사·직접 입력 → 묶음 끝
        }
        return end;
    }
}
