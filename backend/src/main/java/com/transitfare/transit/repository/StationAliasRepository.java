package com.transitfare.transit.repository;

import com.transitfare.transit.entity.StationAlias;
import org.springframework.data.jpa.repository.JpaRepository;

/** 역 다른 이름 (역 매칭 Task 10-A에서 사용) */
public interface StationAliasRepository extends JpaRepository<StationAlias, Long> {
}
