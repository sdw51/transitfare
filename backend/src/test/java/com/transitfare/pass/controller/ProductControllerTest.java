package com.transitfare.pass.controller;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.transitfare.pass.PassFacade;
import com.transitfare.pass.contract.ProductType;
import com.transitfare.pass.contract.ProductView;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    MockMvc mockMvc;
    @MockitoBean
    PassFacade passFacade;

    @Test
    @DisplayName("지역의 승차권 상품을 종류 라벨과 함께 돌려준다")
    void listsProductsByRegion() throws Exception {
        given(passFacade.listProducts("JP-FUKUOKA")).willReturn(List.of(
                new ProductView("FUKUOKA_SUBWAY_1DAY", ProductType.PASS, "후쿠오카 지하철 1일 승차권",
                        "福岡市地下鉄 1日乗車券", 640, 1, null)));

        mockMvc.perform(get("/api/v1/products").param("region", "JP-FUKUOKA"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].code").value("FUKUOKA_SUBWAY_1DAY"))
                .andExpect(jsonPath("$[0].type.code").value("PASS"))
                .andExpect(jsonPath("$[0].type.label").value("패스"))
                .andExpect(jsonPath("$[0].nameKo").value("후쿠오카 지하철 1일 승차권"))
                .andExpect(jsonPath("$[0].price").value(640))
                .andExpect(jsonPath("$[0].validDays").value(1));
    }

    @Test
    @DisplayName("지역 파라미터가 없으면 400 INVALID_INPUT이다")
    void missingRegionIsInvalidInput() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }
}
