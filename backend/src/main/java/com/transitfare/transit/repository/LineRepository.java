package com.transitfare.transit.repository;

import com.transitfare.transit.entity.Line;
import org.springframework.data.jpa.repository.JpaRepository;

/** 노선 */
public interface LineRepository extends JpaRepository<Line, Long> {
}
