package com.transitfare.transit.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.transitfare.transit.contract.MatchedLeg;
import com.transitfare.transit.contract.StepToMatch;
import com.transitfare.transit.contract.TrainClass;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.transaction.annotation.Transactional;

/**
 * 경로 단계 역 매칭. 테스트 데이터(matching-test.sql, 가상):
 * TEST_JR(별칭 "JR九州") 역 博多(1)·二日市(2, 별칭 "ふつかいち"), 노선 鹿児島本線(1)에 둘 다.
 * TEST_RAIL·TEST_BUS 둘 다 별칭 "西日本鉄道" — 전철 역 天神(11)·大橋(12), 버스 정류장 天神(21)·キャナルシティ(22).
 */
@SpringBootTest
@Transactional
@Sql("/transit/matching-test.sql")
class MatchStepUseCaseTest {

    @Autowired
    MatchStepUseCase matchStepUseCase;

    static StepToMatch step(String agency, String line, String train, String from, String to) {
        return new StepToMatch(agency, line, train, from, to);
    }

    @Test
    @DisplayName("운영사 별칭과 역 이름·역 별칭으로 우리 역·노선을 찾는다")
    void matchesByAlias() {
        Optional<MatchedLeg> leg = matchStepUseCase.execute("TEST", step("JR九州", "鹿児島本線", null, "博多", "ふつかいち"));

        assertThat(leg).contains(new MatchedLeg("TEST_JR", 1L, 1L, 2L, TrainClass.LOCAL));
    }

    @Test
    @DisplayName("정확히 같은 이름이 없으면 '駅'·공백·전각을 정규화한 뒤 찾는다")
    void matchesAfterNormalizingStationSuffix() {
        Optional<MatchedLeg> leg = matchStepUseCase.execute("TEST", step("JR九州", null, null, "博多駅", "二日市駅 "));

        assertThat(leg).hasValueSatisfying(l -> {
            assertThat(l.fromStationId()).isEqualTo(1L);
            assertThat(l.toStationId()).isEqualTo(2L);
        });
    }

    @Test
    @DisplayName("모르는 운영사 이름이나 없는 역이면 매칭 실패(빈 값)")
    void unknownAgencyFails() {
        assertThat(matchStepUseCase.execute("TEST", step("知らない会社", null, null, "博多", "二日市"))).isEmpty();
        assertThat(matchStepUseCase.execute("TEST", step("JR九州", null, null, "博多", "小倉"))).isEmpty();
        assertThat(matchStepUseCase.execute("OTHER_REGION", step("JR九州", null, null, "博多", "二日市"))).isEmpty();
    }

    @Test
    @DisplayName("열차 이름이나 노선 이름에 '特急'이 있으면 특급으로 본다")
    void detectsExpressFromTrainName() {
        assertThat(matchStepUseCase.execute("TEST", step("JR九州", "鹿児島本線", "特急かもめ", "博多", "二日市")))
                .hasValueSatisfying(l -> assertThat(l.trainClass()).isEqualTo(TrainClass.EXPRESS));
        assertThat(matchStepUseCase.execute("TEST", step("JR九州", "特急ゆふ", null, "博多", "二日市")))
                .hasValueSatisfying(l -> assertThat(l.trainClass()).isEqualTo(TrainClass.EXPRESS));
    }

    @Test
    @DisplayName("같은 별칭의 운영사가 여럿이면 두 역이 모두 있는 운영사로 정한다")
    void sameAgencyNameResolvedByStations() {
        assertThat(matchStepUseCase.execute("TEST", step("西日本鉄道", null, null, "天神", "大橋")))
                .hasValueSatisfying(l -> assertThat(l.operatorCode()).isEqualTo("TEST_RAIL"));
        assertThat(matchStepUseCase.execute("TEST", step("西日本鉄道", null, null, "天神", "キャナルシティ")))
                .hasValueSatisfying(l -> {
                    assertThat(l.operatorCode()).isEqualTo("TEST_BUS");
                    assertThat(l.lineId()).isNull();   // 버스는 노선 데이터가 없으면 null
                });
    }
}
