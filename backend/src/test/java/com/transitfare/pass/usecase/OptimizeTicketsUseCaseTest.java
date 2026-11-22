package com.transitfare.pass.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.transitfare.pass.contract.ProductOverride;
import com.transitfare.pass.contract.ProductType;
import com.transitfare.pass.contract.Purchase;
import com.transitfare.pass.contract.TicketLeg;
import com.transitfare.pass.contract.TicketParty;
import com.transitfare.pass.entity.TicketCoverage;
import com.transitfare.pass.entity.TicketProduct;
import com.transitfare.pass.repository.TicketCoverageRepository;
import com.transitfare.pass.repository.TicketProductRepository;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/** 시드 데이터와 섞이지 않게 테스트 전용 지역(TEST-REGION)에 상품을 저장한다. */
@SpringBootTest
@Transactional
class OptimizeTicketsUseCaseTest {

    static final String REGION = "TEST-REGION";
    static final LocalDate D1 = LocalDate.of(2026, 12, 1);

    @Autowired
    OptimizeTicketsUseCase useCase;
    @Autowired
    TicketProductRepository productRepository;
    @Autowired
    TicketCoverageRepository coverageRepository;

    // 하루 지하철 4번(210엔 × 4 = 840엔), 1일권 640엔
    final List<TicketLeg> legs = List.of(leg(), leg(), leg(), leg());

    @BeforeEach
    void saveProduct() {
        productRepository.save(TicketProduct.builder().code("T_SUBWAY_1DAY").regionCode(REGION)
                .type(ProductType.PASS).nameJa("1日乗車券").nameKo("1일 승차권").price(640)
                .validDays(1).consecutiveDays(true).uses(0).coversExpress(false).reservedSeatLimit(0)
                .sourceUrl("https://example.com/pass").verifiedOn(D1).build());
        coverageRepository.save(TicketCoverage.builder().productCode("T_SUBWAY_1DAY").operatorCode("T_SUBWAY").build());
    }

    @Test
    @DisplayName("사용자가 제외한 승차권은 추천하지 않는다")
    void overrideExcludedProductIgnored() {
        var result = useCase.execute(REGION, legs, new TicketParty(1),
                List.of(new ProductOverride("T_SUBWAY_1DAY", false, null)));
        assertThat(result.optimizedTotal()).isEqualTo(840);
        assertThat(result.purchases()).isEmpty();
    }

    @Test
    @DisplayName("사용자가 입력한 가격이 공식가를 대신한다")
    void overridePriceReplacesOfficialPrice() {
        var result = useCase.execute(REGION, legs, new TicketParty(1),
                List.of(new ProductOverride("T_SUBWAY_1DAY", true, 500)));
        assertThat(result.optimizedTotal()).isEqualTo(500);
        assertThat(result.purchases()).containsExactly(new Purchase("T_SUBWAY_1DAY", D1, 500, 1));
    }

    private static TicketLeg leg() {
        return new TicketLeg(D1, "T_SUBWAY", null, null, null, false, false, true, 210, 0, 0, null);
    }
}
