package com.transitfare.transit.contract;

/** 일행 인원(어른 수). 어린이 요금은 다루지 않는다. 금액 × adults = 일행 금액 */
public record Passengers(int adults) {
}
