package com.transitfare.transit.repository;

import com.transitfare.transit.entity.LineStation;
import com.transitfare.transit.entity.LineStationId;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 노선별 역과 누적 km */
public interface LineStationRepository extends JpaRepository<LineStation, LineStationId> {

    /** 여러 역이 속한 노선 연결을 한 번에 (역 검색 결과에 노선 붙일 때) */
    List<LineStation> findByIdStationIdIn(Collection<Long> stationIds);
}
