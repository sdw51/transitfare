package com.transitfare.transit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 거리 구간 요금: max_km 이하 거리의 운임(BASE) 또는 특급요금(EXPRESS) */
@Entity
@Table(schema = "transit", name = "distance_fare_band")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class DistanceFareBand {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "operator_code", nullable = false)
    private String operatorCode;

    /** BASE | EXPRESS */
    @Column(name = "kind", nullable = false)
    private String kind;

    @Column(name = "max_km", nullable = false, precision = 6, scale = 1)
    private BigDecimal maxKm;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "source_url", nullable = false)
    private String sourceUrl;

    @Column(name = "verified_on", nullable = false)
    private LocalDate verifiedOn;
}
