package com.transitfare.transit.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 구간 요금 상태. UNKNOWN은 0원이 아니라 계산하지 못한 구간. 응답은 {code, label, description} */
@Getter
@RequiredArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum FareStatus {
    OK("확인됨", "자체 요금 데이터로 계산한 요금"),
    UNKNOWN("미확인", "요금 데이터에 없어 계산하지 못한 구간"),
    MANUAL("직접 입력", "사용자가 입력한 요금");

    private final String label;
    private final String description;

    @JsonProperty("code")
    public String getCode() {
        return name();
    }

    /** 요청 값: 코드 문자열 "OK" 또는 응답으로 받은 객체 {"code": "OK", ...}. 모르는 코드면 예외 → 400 INVALID_INPUT */
    @JsonCreator
    public static FareStatus from(Object value) {
        String code = value instanceof Map<?, ?> map ? (String) map.get("code") : (String) value;
        return valueOf(code);
    }
}
