package com.transitfare.transit.entity;

import com.transitfare.transit.domain.FareRuleType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 운영사. 요금 계산 방식(fareRuleType)과 요금 기준 설명을 가진다 */
@Entity
@Table(schema = "transit", name = "operator")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Operator {

    @Id
    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "region_code", nullable = false)
    private String regionCode;

    @Column(name = "name_ja", nullable = false)
    private String nameJa;

    @Column(name = "name_ko", nullable = false)
    private String nameKo;

    @Enumerated(EnumType.STRING)
    @Column(name = "fare_rule_type", nullable = false)
    private FareRuleType fareRuleType;

    @Column(name = "fare_basis_note")
    private String fareBasisNote;
}
