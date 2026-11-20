package com.transitfare.pass.contract;

/** 화면 승차권 목록 한 줄. price는 어른 1명 공식가. */
public record ProductView(String code, ProductType type, String nameKo, String nameJa,
                          int price, int validDays, String eligibilityNote) {
}
