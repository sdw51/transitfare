package com.transitfare.transit.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

/**
 * 지역·운영사·역 조회 API. 실제 스프링 컨텍스트(모듈별 Flyway로 만든 H2)에 테스트 전용 데이터(station-api-test.sql)를 넣고
 * MockMvc로 부른다. @Transactional이라 테스트마다 데이터가 롤백된다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Sql("/transit/station-api-test.sql")
class StationControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("역 검색은 별칭과 한국어 이름으로도 찾고 노선을 붙여 준다")
    void searchMatchesAliasAndKoreanName() throws Exception {
        // 별칭 "Hakata"(대소문자 무시) → 博多
        mockMvc.perform(get("/api/v1/stations").param("region", "TEST").param("q", "hakata"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].nameJa").value("博多"))
                .andExpect(jsonPath("$[0].operatorCode").value("TEST_SUBWAY"))
                .andExpect(jsonPath("$[0].lines[0].nameKo").value("공항선"));
        // 한국어 이름 일부 "텐진" → 지하철 天神·天神南 + 버스 天神 = 3개
        mockMvc.perform(get("/api/v1/stations").param("region", "TEST").param("q", "텐진"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3));
        // 운영사로 좁히기: 버스 운영사에는 "텐진" 정류장 하나
        mockMvc.perform(get("/api/v1/stations").param("region", "TEST").param("operator", "TEST_BUS").param("q", "텐진"))
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].operatorCode").value("TEST_BUS"));
    }

    @Test
    @DisplayName("지역의 운영사 목록을 코드 순으로 돌려준다")
    void listsOperatorsByRegion() throws Exception {
        mockMvc.perform(get("/api/v1/operators").param("region", "TEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("TEST_BUS"))
                .andExpect(jsonPath("$[1].code").value("TEST_SUBWAY"))
                .andExpect(jsonPath("$[1].fareBasisNote").value("현금 요금 기준"));
    }

    @Test
    @DisplayName("지역 정보를 돌려준다")
    void getsRegion() throws Exception {
        mockMvc.perform(get("/api/v1/regions/{code}", "TEST"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("TEST"))
                .andExpect(jsonPath("$.currency").value("JPY"))
                .andExpect(jsonPath("$.timezone").value("Asia/Tokyo"));
    }

    @Test
    @DisplayName("없는 지역은 404 TRANSIT_REGION_NOT_FOUND")
    void unknownRegionReturns404() throws Exception {
        mockMvc.perform(get("/api/v1/regions/{code}", "NOWHERE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRANSIT_REGION_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/stations").param("region", "NOWHERE").param("q", "a"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRANSIT_REGION_NOT_FOUND"));
        mockMvc.perform(get("/api/v1/operators").param("region", "NOWHERE"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TRANSIT_REGION_NOT_FOUND"));
    }

    @Test
    @DisplayName("region 파라미터가 없으면 400 INVALID_INPUT")
    void missingRegionIsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/stations").param("q", "a"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }
}
