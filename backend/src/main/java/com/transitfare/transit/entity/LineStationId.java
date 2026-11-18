package com.transitfare.transit.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import java.io.Serializable;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@EqualsAndHashCode
public class LineStationId implements Serializable {

    @Column(name = "line_id", nullable = false)
    private Long lineId;

    @Column(name = "station_id", nullable = false)
    private Long stationId;
}
