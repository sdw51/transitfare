package com.transitfare.transit.repository;

import com.transitfare.transit.entity.DistanceFareBand;
import java.math.BigDecimal;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 거리 구간 요금 */
public interface DistanceFareBandRepository extends JpaRepository<DistanceFareBand, Long> {

    /** km 이상인 밴드 중 max_km가 가장 작은 것 */
    Optional<DistanceFareBand> findFirstByOperatorCodeAndKindAndMaxKmGreaterThanEqualOrderByMaxKmAsc(
            String operatorCode, String kind, BigDecimal km);
}
