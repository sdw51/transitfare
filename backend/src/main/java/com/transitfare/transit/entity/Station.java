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

/** 역·정류장. 같은 실제 역도 운영사별로 따로 둔다 */
@Entity
@Table(schema = "transit", name = "station")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Station {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "region_code", nullable = false)
    private String regionCode;

    @Column(name = "operator_code", nullable = false)
    private String operatorCode;

    @Column(name = "name_ja", nullable = false)
    private String nameJa;

    @Column(name = "name_ko")
    private String nameKo;

    @Column(name = "source_url", nullable = false)
    private String sourceUrl;

    @Column(name = "verified_on", nullable = false)
    private LocalDate verifiedOn;
}
