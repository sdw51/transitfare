package com.transitfare.transit.repository;

import com.transitfare.transit.entity.FlatZoneStation;
import com.transitfare.transit.entity.FlatZoneStationId;
import org.springframework.data.jpa.repository.JpaRepository;

/** 균일 구간에 속한 정류장 */
public interface FlatZoneStationRepository extends JpaRepository<FlatZoneStation, FlatZoneStationId> {
}
