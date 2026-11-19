package com.transitfare.transit.config;

import com.transitfare.transit.domain.FareData;
import com.transitfare.transit.domain.FareEngine;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 요금 계산 엔진을 빈으로 등록한다. FareEngine은 domain(순수 Java)이라 스스로 @Component를 달 수 없어서
 * 여기서 FareData 구현(JpaFareData)을 넣어 만든다.
 */
@Configuration
public class FareEngineConfig {

    @Bean
    public FareEngine fareEngine(FareData fareData) {
        return new FareEngine(fareData);
    }
}
