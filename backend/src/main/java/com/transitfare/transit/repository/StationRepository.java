package com.transitfare.transit.repository;

import com.transitfare.transit.entity.Station;
import java.util.List;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** 역·정류장 */
public interface StationRepository extends JpaRepository<Station, Long> {

    /**
     * 역 검색: 지역 안에서(운영사가 주어지면 그 운영사만) 일본어 이름·한국어 이름·별칭 중 하나라도
     * pattern(소문자, %…% 형태)을 포함하는 역. 대소문자 무시, id 순, limit개까지.
     */
    @Query("""
            select s from Station s
            where s.regionCode = :regionCode
              and (:operatorCode is null or s.operatorCode = :operatorCode)
              and (lower(s.nameJa) like :pattern
                   or lower(s.nameKo) like :pattern
                   or exists (select 1 from StationAlias a where a.stationId = s.id and lower(a.alias) like :pattern))
            order by s.id
            """)
    List<Station> search(String regionCode, String operatorCode, String pattern, Limit limit);
}
