package com.transitfare.common.error;

import org.springframework.http.HttpStatus;

/** 모듈별 에러 코드 enum이 구현한다. 메서드 이름은 Lombok @Getter가 만드는 이름에 맞춘다. */
public interface ErrorCode {

    HttpStatus getStatus();

    String getMessage();

    String name();
}
