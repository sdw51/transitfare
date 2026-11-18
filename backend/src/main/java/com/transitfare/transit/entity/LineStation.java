package com.transitfare.transit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** 노선에 속한 역과 그 노선 기점부터의 누적 km */
@Entity
@Table(schema = "transit", name = "line_station")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class LineStation {

    @EmbeddedId
    private LineStationId id;

    @Column(name = "cumulative_km", nullable = false, precision = 6, scale = 1)
    private BigDecimal cumulativeKm;
}
