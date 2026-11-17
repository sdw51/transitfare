package com.transitfare.common.error;

/** 모든 에러 응답 본문. status는 HTTP 상태와 같은 값, field는 입력칸과 관련 없으면 null. */
public record ApiError(int status, String code, String message, String field) {
}
