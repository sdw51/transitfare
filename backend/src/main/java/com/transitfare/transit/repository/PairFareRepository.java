package com.transitfare.transit.repository;

import com.transitfare.transit.entity.PairFare;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** 역 쌍 요금 */
public interface PairFareRepository extends JpaRepository<PairFare, Long> {

    /** (a, b) 또는 (b, a)로 저장된 쌍 요금 — 양방향 동일 */
    @Query("""
            select p from PairFare p
            where p.operatorCode = :operatorCode and p.kind = :kind
              and ((p.stationAId = :a and p.stationBId = :b) or (p.stationAId = :b and p.stationBId = :a))
            """)
    Optional<PairFare> findPair(String operatorCode, String kind, long a, long b);
}
