package com.transitfare.transit.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class StationNameNormalizerTest {

    @Test
    @DisplayName("역 이름 끝의 '駅'과 공백을 없애고 전각 영숫자를 반각으로 바꾼다")
    void stripsSuffixAndWidth() {
        assertThat(StationNameNormalizer.normalize("博多駅 ")).isEqualTo("博多");
        assertThat(StationNameNormalizer.normalize("ＪＲ")).isEqualTo("JR");
        // 전각 공백(U+3000)도 공백으로 본다
        assertThat(StationNameNormalizer.normalize("天神　南")).isEqualTo("天神南");
        // 괄호 같은 기호는 바꾸지 않는다 (영숫자만 반각으로)
        assertThat(StationNameNormalizer.normalize("福岡空港（国際線）")).isEqualTo("福岡空港（国際線）");
        // '駅'은 이름 끝에 있을 때만 뗀다
        assertThat(StationNameNormalizer.normalize("駅前３丁目")).isEqualTo("駅前3丁目");
    }

    @Test
    @DisplayName("null이면 빈 문자열을 돌려준다")
    void nullBecomesEmpty() {
        assertThat(StationNameNormalizer.normalize(null)).isEmpty();
    }
}
