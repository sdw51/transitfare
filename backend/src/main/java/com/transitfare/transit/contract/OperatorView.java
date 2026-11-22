package com.transitfare.transit.contract;

/** 운영사 정보. fareBasisNote는 화면에 보이는 요금 기준 설명(예: 현금 요금 기준) */
public record OperatorView(String code, String nameJa, String nameKo, String fareBasisNote) {
}
