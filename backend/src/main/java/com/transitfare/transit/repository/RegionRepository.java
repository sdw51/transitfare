package com.transitfare.transit.repository;

import com.transitfare.transit.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;

/** 지역 */
public interface RegionRepository extends JpaRepository<Region, String> {
}
