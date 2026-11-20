package com.transitfare.pass.contract;

/** 여행별 사용자 설정. included=false면 비교에서 제외, priceAmount가 null이면 공식가 */
public record ProductOverride(String productCode, boolean included, Integer priceAmount) {
}
