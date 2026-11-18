package com.transitfare.transit.domain;

/**
 * 요금 계산에 필요한 운영사 정보.
 *
 * @param code     운영사 코드(operator.code, 예: JR_KYUSHU)
 * @param ruleType 요금 계산 방식
 */
public record OperatorRule(String code, FareRuleType ruleType) {
}
