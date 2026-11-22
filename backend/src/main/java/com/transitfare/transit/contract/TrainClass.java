package com.transitfare.transit.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 열차 종별. EXPRESS면 운임에 특급요금이 더해진다. 응답은 {code, label, description} */
@Getter
@RequiredArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum TrainClass {
    LOCAL("보통", "운임만 내는 열차"),
    EXPRESS("특급", "운임에 특급요금이 더해지는 열차");

    private final String label;
    private final String description;

    @JsonProperty("code")
    public String getCode() {
        return name();
    }

    /** 요청 값: 코드 문자열 "EXPRESS" 또는 응답으로 받은 객체 {"code": "EXPRESS", ...}. 모르는 코드면 예외 → 400 INVALID_INPUT */
    @JsonCreator
    public static TrainClass from(Object value) {
        String code = value instanceof Map<?, ?> map ? (String) map.get("code") : (String) value;
        return valueOf(code);
    }
}
