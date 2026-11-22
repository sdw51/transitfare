package com.transitfare.transit.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 구간 종류. 응답은 {code, label, description} */
@Getter
@RequiredArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum LegMode {
    TRANSIT("대중교통", "열차·버스처럼 요금이 드는 구간"),
    WALK("도보", "요금이 없는 걷는 구간"),
    MANUAL("직접 입력", "요금을 직접 입력한 구간");

    private final String label;
    private final String description;

    @JsonProperty("code")
    public String getCode() {
        return name();
    }

    /** 요청 값: 코드 문자열 "TRANSIT" 또는 응답으로 받은 객체 {"code": "TRANSIT", ...}. 모르는 코드면 예외 → 400 INVALID_INPUT */
    @JsonCreator
    public static LegMode from(Object value) {
        String code = value instanceof Map<?, ?> map ? (String) map.get("code") : (String) value;
        return valueOf(code);
    }
}
