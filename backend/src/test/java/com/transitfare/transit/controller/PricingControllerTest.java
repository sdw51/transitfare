package com.transitfare.transit.controller;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
 * 구간 요금 계산 API. 실제 컨텍스트 + 테스트 전용 데이터(pricing-test.sql: 지역 TEST 지하철 3.0km 이하 210)로 MockMvc 호출.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Sql("/transit/pricing-test.sql")
class PricingControllerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("구간 요금과 일행 총액을 돌려주고, enum은 code·label 객체로 나간다")
    void priceEndpointReturnsPricingResult() throws Exception {
        // mode는 코드 문자열, trainClass는 응답으로 받은 객체 모양 — 둘 다 받아야 한다
        String body = """
                {"regionCode":"TEST","passengers":{"adults":2},
                 "legs":[{"mode":"TRANSIT","operatorCode":"TEST_SUBWAY","lineId":1,"fromStationId":1,"toStationId":2,
                          "trainClass":{"code":"LOCAL","label":"보통","description":"운임만 내는 열차"},
                          "seatType":"UNRESERVED","manualAmount":null}]}
                """;
        mockMvc.perform(post("/api/v1/pricing").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.legs[0].base").value(210))
                .andExpect(jsonPath("$.legs[0].status.code").value("OK"))
                .andExpect(jsonPath("$.legs[0].status.label").value("확인됨"))
                .andExpect(jsonPath("$.partyTotal").value(420))
                .andExpect(jsonPath("$.hasUnknown").value(false));
    }

    @Test
    @DisplayName("인원이 0명이면 400 TRANSIT_INVALID_PASSENGERS")
    void zeroPassengersRejected() throws Exception {
        String body = """
                {"regionCode":"TEST","passengers":{"adults":0},
                 "legs":[{"mode":"WALK"}]}
                """;
        mockMvc.perform(post("/api/v1/pricing").contentType(APPLICATION_JSON).content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("TRANSIT_INVALID_PASSENGERS"))
                .andExpect(jsonPath("$.message").value("인원을 1명 이상 입력하세요"))
                .andExpect(jsonPath("$.field").value("passengers.adults"));
    }

    @Test
    @DisplayName("모르는 enum 코드나 빠진 필수값은 400 INVALID_INPUT")
    void invalidBodyRejected() throws Exception {
        String unknownMode = """
                {"regionCode":"TEST","passengers":{"adults":1},"legs":[{"mode":"FLY"}]}
                """;
        mockMvc.perform(post("/api/v1/pricing").contentType(APPLICATION_JSON).content(unknownMode))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        String noLegs = """
                {"regionCode":"TEST","passengers":{"adults":1}}
                """;
        mockMvc.perform(post("/api/v1/pricing").contentType(APPLICATION_JSON).content(noLegs))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.field").value("legs"));
    }
}
