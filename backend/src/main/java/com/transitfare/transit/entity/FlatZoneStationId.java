package com.transitfare.transit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 균일 구간에 속한 정류장 — 기본키(zone_id, station_id) */
@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode
public class FlatZoneStationId implements Serializable {

    @Column(name = "zone_id", nullable = false)
    private Long zoneId;

    @Column(name = "station_id", nullable = false)
    private Long stationId;
}
