package com.transitfare.pass.entity;

import com.transitfare.pass.contract.ProductType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 승차권 상품(패스·왕복권·회수권). 가격은 어른 1명 기준, 지역 통화 정수. */
@Entity
@Table(schema = "pass", name = "ticket_product")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class TicketProduct {

    @Id
    @Column(name = "code")
    private String code;
    @Column(name = "region_code", nullable = false)
    private String regionCode;
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private ProductType type;
    @Column(name = "name_ja", nullable = false)
    private String nameJa;
    @Column(name = "name_ko", nullable = false)
    private String nameKo;
    @Column(name = "price", nullable = false)
    private int price;
    /** PASS: 사용 일수, FIXED_ROUTE: 첫 사용일부터 유효 일수 */
    @Column(name = "valid_days", nullable = false)
    private int validDays;
    @Column(name = "consecutive_days", nullable = false)
    private boolean consecutiveDays;
    /** FIXED_ROUTE 승차 횟수. PASS는 0 = 무제한 */
    @Column(name = "uses", nullable = false)
    private int uses;
    @Column(name = "covers_express", nullable = false)
    private boolean coversExpress;
    /** 지정석 요금을 커버하는 횟수. 0이면 커버 안 함 */
    @Column(name = "reserved_seat_limit", nullable = false)
    private int reservedSeatLimit;
    @Column(name = "eligibility_note")
    private String eligibilityNote;
    @Column(name = "source_url", nullable = false)
    private String sourceUrl;
    @Column(name = "verified_on", nullable = false)
    private LocalDate verifiedOn;
}
