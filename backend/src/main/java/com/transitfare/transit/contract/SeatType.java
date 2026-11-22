package com.transitfare.transit.contract;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Map;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** 좌석 종류. RESERVED면 지정석 요금이 더해진다. 응답은 {code, label, description} */
@Getter
@RequiredArgsConstructor
@JsonFormat(shape = JsonFormat.Shape.OBJECT)
public enum SeatType {
    UNRESERVED("자유석", "좌석을 지정하지 않는 자리"),
    RESERVED("지정석", "지정석 요금을 내고 예약하는 자리");

    private final String label;
    private final String description;

    @JsonProperty("code")
    public String getCode() {
        return name();
    }

    /** 요청 값: 코드 문자열 "RESERVED" 또는 응답으로 받은 객체 {"code": "RESERVED", ...}. 모르는 코드면 예외 → 400 INVALID_INPUT */
    @JsonCreator
    public static SeatType from(Object value) {
        String code = value instanceof Map<?, ?> map ? (String) map.get("code") : (String) value;
        return valueOf(code);
    }
}
