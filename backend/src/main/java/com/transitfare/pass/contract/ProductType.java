package com.transitfare.pass.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 승차권 상품 종류. 응답 JSON은 {code, label, description}. */
@Getter
@RequiredArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum ProductType {
    PASS("패스", "정해진 기간 동안 대상 노선을 자유롭게 타는 승차권"),
    FIXED_ROUTE("구간 승차권", "정해진 구간을 정해진 횟수만큼 타는 왕복권·회수권");

    private final String label;
    private final String description;

    @JsonProperty("code")
    public String getCode() {
        return name();
    }

    /** 요청 값: 코드 문자열 "PASS" 또는 응답으로 받은 객체 {"code": "PASS", ...}. 모르는 코드면 예외 → 400 INVALID_INPUT. */
    @JsonCreator
    public static ProductType from(Object value) {
        String code = value instanceof Map<?, ?> map ? (String) map.get("code") : (String) value;
        return valueOf(code);
    }
}
