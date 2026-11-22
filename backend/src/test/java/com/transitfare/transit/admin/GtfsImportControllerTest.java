package com.transitfare.transit.admin;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.transitfare.transit.usecase.GtfsZip;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/** GTFS 가져오기 관리 API. 지역 TEST·운영사 TEST_BUS(gtfs-test.sql)에 테스트 안에서 만든 zip을 올린다 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Sql("/transit/gtfs-test.sql")
class GtfsImportControllerTest {

    static final byte[] FEED = GtfsZip.bytes("""
            stop_id,stop_name,zone_id
            S1,天神,Z1
            S2,博多駅,Z2
            """, """
            fare_id,price,currency_type,payment_method,transfers
            F1,210,JPY,0,0
            """, """
            fare_id,route_id,origin_id,destination_id,contains_id
            F1,,Z1,Z2,
            """);

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("GTFS zip을 올리면 넣은 정류장·요금 수와 건너뛴 사유를 돌려준다")
    void uploadReturnsCounts() throws Exception {
        mockMvc.perform(multipart("/api/v1/admin/transit/import/gtfs")
                        .file(new MockMultipartFile("file", "gtfs.zip", "application/zip", FEED))
                        .param("operator", "TEST_BUS")
                        .param("sourceUrl", "https://example.test/gtfs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.stationsUpserted").value(2))
                .andExpect(jsonPath("$.faresUpserted").value(1))
                .andExpect(jsonPath("$.skipped.length()").value(0));
    }

    @Test
    @DisplayName("없는 운영사는 404, 파일이 없거나 GTFS가 아니면 400")
    void invalidUploadRejected() throws Exception {
        mockMvc.perform(multipart("/api/v1/admin/transit/import/gtfs")
                        .file(new MockMultipartFile("file", "gtfs.zip", "application/zip", FEED))
                        .param("operator", "NOPE")
                        .param("sourceUrl", "https://example.test/gtfs"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRANSIT_OPERATOR_NOT_FOUND"));
        mockMvc.perform(multipart("/api/v1/admin/transit/import/gtfs")
                        .param("operator", "TEST_BUS")
                        .param("sourceUrl", "https://example.test/gtfs"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TRANSIT_GTFS_INVALID"));
        mockMvc.perform(multipart("/api/v1/admin/transit/import/gtfs")
                        .file(new MockMultipartFile("file", "gtfs.zip", "application/zip", FEED))
                        .param("operator", "TEST_BUS")
                        .param("sourceUrl", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TRANSIT_SOURCE_REQUIRED"));
    }
}
