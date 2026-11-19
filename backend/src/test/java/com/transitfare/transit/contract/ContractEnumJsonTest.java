package com.transitfare.transit.contract;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** contract 코드 enum의 JSON 모양: 응답은 {code,label,description}, 요청은 코드 문자열·객체 둘 다 */
class ContractEnumJsonTest {

    final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    @DisplayName("코드 enum은 code·label·description 객체로 나간다")
    void serializesAsCodeLabelDescription() {
        JsonNode node = mapper.readTree(mapper.writeValueAsString(TrainClass.EXPRESS));
        assertThat(node.get("code").asString()).isEqualTo("EXPRESS");
        assertThat(node.get("label").asString()).isEqualTo("특급");
        assertThat(node.get("description").asString()).isEqualTo("운임에 특급요금이 더해지는 열차");
        assertThat(node.size()).isEqualTo(3);
        // 요금 결과 안의 status도 같은 모양
        JsonNode fare = mapper.readTree(mapper.writeValueAsString(
                new LegFare(0, FareStatus.UNKNOWN, 0, 0, 0, 0, "JR_KYUSHU", "운임 미확인", null)));
        assertThat(fare.get("status").get("label").asString()).isEqualTo("미확인");
    }

    @Test
    @DisplayName("요청에서는 코드 문자열과 응답으로 받은 객체를 둘 다 읽는다")
    void readsCodeStringOrObject() {
        assertThat(mapper.readValue("\"EXPRESS\"", TrainClass.class)).isEqualTo(TrainClass.EXPRESS);
        assertThat(mapper.readValue("{\"code\":\"RESERVED\",\"label\":\"지정석\"}", SeatType.class))
                .isEqualTo(SeatType.RESERVED);
        LegInput leg = mapper.readValue("""
                {"mode":"TRANSIT","operatorCode":"JR_KYUSHU","lineId":1,"fromStationId":2,"toStationId":3,
                 "trainClass":{"code":"LOCAL","label":"보통","description":"운임만 내는 열차"},
                 "seatType":"UNRESERVED","manualAmount":null}
                """, LegInput.class);
        assertThat(leg.mode()).isEqualTo(LegMode.TRANSIT);
        assertThat(leg.trainClass()).isEqualTo(TrainClass.LOCAL);
        assertThat(mapper.readValue("\"OK\"", FareStatus.class)).isEqualTo(FareStatus.OK);
    }

    @Test
    @DisplayName("모르는 코드는 읽기 실패(컨트롤러에서 400 INVALID_INPUT)")
    void unknownCodeRejected() {
        assertThatThrownBy(() -> mapper.readValue("\"FAST\"", TrainClass.class))
                .isInstanceOf(JacksonException.class);
    }
}
