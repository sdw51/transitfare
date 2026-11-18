package com.transitfare.transit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 역 쌍 요금(양방향 동일). 거리제 운영사에서는 특정 구간 예외로 밴드보다 먼저 쓴다 */
@Entity
@Table(schema = "transit", name = "pair_fare")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class PairFare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "operator_code", nullable = false)
    private String operatorCode;

    @Column(name = "station_a_id", nullable = false)
    private Long stationAId;

    @Column(name = "station_b_id", nullable = false)
    private Long stationBId;

    /** BASE | EXPRESS */
    @Column(name = "kind", nullable = false)
    private String kind;

    @Column(name = "amount", nullable = false)
    private Integer amount;

    @Column(name = "source_url", nullable = false)
    private String sourceUrl;

    @Column(name = "verified_on", nullable = false)
    private LocalDate verifiedOn;
}
