package com.transitfare.transit;

import com.transitfare.transit.usecase.dto.GtfsImportResult;
import java.io.InputStream;

/**
 * transit 관리 기능의 입구. 자기 모듈의 admin 컨트롤러만 쓴다(다른 모듈은 쓰지 않음 — 메인 세션 결정).
 * 메서드는 관리 기능이 늘며 추가된다(Task 15 관리 CRUD).
 */
public interface TransitAdminFacade {

    /**
     * 운영사의 GTFS zip에서 정류장과 구역 간 요금을 가져온다(멱등). 없는 운영사 → TRANSIT_OPERATOR_NOT_FOUND,
     * 출처 없음 → TRANSIT_SOURCE_REQUIRED, 읽을 수 없는 파일 → TRANSIT_GTFS_INVALID.
     */
    GtfsImportResult importGtfsFares(String operatorCode, String sourceUrl, InputStream zip);
}
