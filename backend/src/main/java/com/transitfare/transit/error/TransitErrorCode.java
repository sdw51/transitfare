package com.transitfare.transit.error;

import com.transitfare.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** transit 모듈 에러 코드. 처음 던지는 커밋에 추가한다(계획 "에러 코드" 표). */
@Getter
@RequiredArgsConstructor
public enum TransitErrorCode implements ErrorCode {
    TRANSIT_REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "지역을 찾을 수 없습니다");

    private final HttpStatus status;
    private final String message;
}
