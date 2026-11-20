package com.transitfare.pass.contract;

/** 승차권 비교 인원. 일행은 항상 같이 움직인다. */
public record TicketParty(int adults) {
}
