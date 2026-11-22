package com.transitfare.transit.admin;

import com.transitfare.transit.TransitAdminFacade;
import com.transitfare.transit.usecase.dto.GtfsImportResult;
import java.io.IOException;
import java.io.InputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * GTFS 요금 가져오기 관리 API (정류장·요금을 DB에 저장한다).
 *
 * <p>부르는 곳: 관리자. 지금은 Swagger UI(/swagger-ui)나 curl로 직접 올리고, Task 15에서 관리자 화면(AdminPage)의
 * 업로드 버튼이 부른다. 일반 사용자 화면과 다른 모듈은 부르지 않는다.
 * 관리 기능 입구인 {@link TransitAdminFacade}를 거친다.
 */
@RestController
@RequestMapping("/api/v1/admin/transit")
@RequiredArgsConstructor
public class GtfsImportController {

    private final TransitAdminFacade transitAdminFacade;

    /**
     * GTFS 가져오기. POST /api/v1/admin/transit/import/gtfs?operator=SHOWA_BUS&sourceUrl=… (multipart file)
     *
     * <p>언제: 관리자가 운영사의 공식 GTFS(쇼와 버스 등)를 새로 받았을 때. 같은 파일을 다시 올려도 행이 늘지 않고
     * 바뀐 요금만 갱신된다.
     * {@link TransitAdminFacade#importGtfsFares}: 같은 이름 정류장을 한 역으로 합쳐 넣고, 구역 간 요금을 역 쌍 요금으로
     * 넣거나 갱신한다. 구역 방식 규칙·요금 파일 없음은 skipped에 사유로 남긴다.
     *
     * @param operator  운영사 코드(예: SHOWA_BUS)
     * @param sourceUrl GTFS를 받은 공식 배포 페이지 URL(요금 출처로 기록)
     * @param file      GTFS zip. 빠지면 빈 파일로 보고 TRANSIT_GTFS_INVALID
     * @return 넣거나 갱신한 정류장 수·요금 수, 건너뛴 사유
     * @throws com.transitfare.common.error.BusinessException
     *         TRANSIT_OPERATOR_NOT_FOUND(404, "운영사를 찾을 수 없습니다"),
     *         TRANSIT_SOURCE_REQUIRED(400, "출처와 확인 날짜를 입력하세요"),
     *         TRANSIT_GTFS_INVALID(400, "GTFS 파일을 읽을 수 없습니다")
     */
    @PostMapping(value = "/import/gtfs", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public GtfsImportResult importGtfs(@RequestParam("operator") String operator,
                                       @RequestParam("sourceUrl") String sourceUrl,
                                       @RequestPart(value = "file", required = false) MultipartFile file)
            throws IOException {
        // 파일 칸이 빠졌을 때 공통 에러 처리(common, 수정 금지)가 500으로 응답하지 않도록, 빈 내용으로 넘겨
        // 유스케이스가 TRANSIT_GTFS_INVALID(400)로 응답하게 한다
        try (InputStream zip = file == null ? InputStream.nullInputStream() : file.getInputStream()) {
            return transitAdminFacade.importGtfsFares(operator, sourceUrl, zip);
        }
    }
}
