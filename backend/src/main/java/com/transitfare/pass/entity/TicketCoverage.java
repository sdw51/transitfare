package com.transitfare.pass.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 상품의 적용 범위 한 줄. lineId가 null이면 운영사 전 노선, 역 두 개가 있으면 양 끝 역 쌍(방향 무관). transit ID는 값만 저장. */
@Entity
@Table(schema = "pass", name = "ticket_coverage")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class TicketCoverage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;
    @Column(name = "product_code", nullable = false)
    private String productCode;
    @Column(name = "operator_code", nullable = false)
    private String operatorCode;
    @Column(name = "line_id")
    private Long lineId;
    @Column(name = "station_a_id")
    private Long stationAId;
    @Column(name = "station_b_id")
    private Long stationBId;
}
