package com.transitfare.transit.repository;

import com.transitfare.transit.entity.FlatZone;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** 균일 구간 */
public interface FlatZoneRepository extends JpaRepository<FlatZone, Long> {

    /** 두 정류장을 모두 포함하는 그 운영사의 균일 구간들(id 순) */
    @Query("""
            select z from FlatZone z
            where z.operatorCode = :operatorCode
              and exists (select 1 from FlatZoneStation s where s.id.zoneId = z.id and s.id.stationId = :a)
              and exists (select 1 from FlatZoneStation s where s.id.zoneId = z.id and s.id.stationId = :b)
            order by z.id
            """)
    List<FlatZone> findZonesContainingBoth(String operatorCode, long a, long b);
}
