package com.transitfare.common;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.transitfare.common.error.BusinessException;
import com.transitfare.common.error.ErrorCode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.TestController.class)
@Import(GlobalExceptionHandlerTest.TestController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mockMvc;

    @Test
    @DisplayName("비즈니스 예외는 에러 코드의 HTTP 상태와 본문으로 응답한다")
    void businessExceptionMapsStatusAndBody() throws Exception {
        mockMvc.perform(get("/test/business"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("TEST_CONFLICT"))
                .andExpect(jsonPath("$.message").value("테스트 충돌입니다"))
                .andExpect(jsonPath("$.field").value("name"));
    }

    @Test
    @DisplayName("입력값 검증 실패는 INVALID_INPUT과 첫 오류 필드로 응답한다")
    void validationErrorMapsToInvalidInput() throws Exception {
        mockMvc.perform(post("/test/validation").contentType(APPLICATION_JSON).content("{\"name\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"))
                .andExpect(jsonPath("$.field").value("name"));
    }

    @Test
    @DisplayName("예상 못 한 예외는 500으로 응답하고 내부 메시지를 숨긴다")
    void unexpectedErrorMapsToInternalError() throws Exception {
        mockMvc.perform(get("/test/unexpected"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.message").value("서버 오류가 발생했습니다"))
                .andExpect(content().string(not(containsString("내부 비밀"))));
    }

    @Getter
    @RequiredArgsConstructor
    enum TestErrorCode implements ErrorCode {
        TEST_CONFLICT(HttpStatus.CONFLICT, "테스트 충돌입니다");

        private final HttpStatus status;
        private final String message;
    }

    record NameRequest(@NotBlank String name) {
    }

    @RestController
    static class TestController {

        @GetMapping("/test/business")
        void business() {
            throw new BusinessException(TestErrorCode.TEST_CONFLICT, "name");
        }

        @PostMapping("/test/validation")
        void validation(@Valid @RequestBody NameRequest request) {
        }

        @GetMapping("/test/unexpected")
        void unexpected() {
            throw new IllegalStateException("내부 비밀");
        }
    }
}
