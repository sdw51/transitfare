package com.transitfare.pass.contract;

import java.time.LocalDate;

/** 추천 구매 하나. startDate: 패스 시작일(왕복권·회수권은 첫 사용일), count: 인원수. */
public record Purchase(String productCode, LocalDate startDate, int unitPrice, int count) {
}
