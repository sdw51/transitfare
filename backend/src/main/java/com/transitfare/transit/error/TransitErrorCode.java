package com.transitfare.transit.error;

import com.transitfare.common.error.ErrorCode;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

/** transit 모듈 에러 코드. 처음 던지는 커밋에 추가한다(계획 "에러 코드" 표). */
@Getter
@RequiredArgsConstructor
public enum TransitErrorCode implements ErrorCode {
    TRANSIT_REGION_NOT_FOUND(HttpStatus.NOT_FOUND, "지역을 찾을 수 없습니다"),
    TRANSIT_INVALID_PASSENGERS(HttpStatus.BAD_REQUEST, "인원을 1명 이상 입력하세요"),
    TRANSIT_OPERATOR_REGION_MISMATCH(HttpStatus.BAD_REQUEST, "다른 지역의 운영사가 섞여 있습니다"),
    TRANSIT_OPERATOR_NOT_FOUND(HttpStatus.NOT_FOUND, "운영사를 찾을 수 없습니다"),
    TRANSIT_GTFS_INVALID(HttpStatus.BAD_REQUEST, "GTFS 파일을 읽을 수 없습니다"),
    TRANSIT_SOURCE_REQUIRED(HttpStatus.BAD_REQUEST, "출처와 확인 날짜를 입력하세요");

    private final HttpStatus status;
    private final String message;
}
