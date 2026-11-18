package com.transitfare.transit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(schema = "transit", name = "region")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class Region {

    @Id
    @Column(name = "code", nullable = false)
    private String code;

    @Column(name = "name_ko", nullable = false)
    private String nameKo;

    @Column(name = "name_ja", nullable = false)
    private String nameJa;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "timezone", nullable = false)
    private String timezone;
}
