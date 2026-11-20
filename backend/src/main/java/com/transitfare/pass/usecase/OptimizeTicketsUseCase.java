package com.transitfare.pass.usecase;

import com.transitfare.pass.contract.OptimizationResult;
import com.transitfare.pass.contract.ProductOverride;
import com.transitfare.pass.contract.TicketLeg;
import com.transitfare.pass.contract.TicketParty;
import com.transitfare.pass.domain.ProductSpec;
import com.transitfare.pass.domain.ProductSpec.Coverage;
import com.transitfare.pass.domain.TicketOptimizer;
import com.transitfare.pass.entity.TicketCoverage;
import com.transitfare.pass.entity.TicketProduct;
import com.transitfare.pass.repository.TicketCoverageRepository;
import com.transitfare.pass.repository.TicketProductRepository;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사용자가 여행 요약에서 가장 싼 승차권 조합 추천을 본다. */
@Service
@RequiredArgsConstructor
public class OptimizeTicketsUseCase {

    private final TicketProductRepository productRepository;
    private final TicketCoverageRepository coverageRepository;
    private final TicketOptimizer optimizer = new TicketOptimizer();

    /**
     * 지역의 상품에 사용자 설정을 적용해서({@code included=false}는 제외, {@code priceAmount}는 공식가 대신)
     * {@link TicketOptimizer}로 가장 싼 구매 조합을 계산한다.
     *
     * @param regionCode 지역 코드
     * @param legs       승차권 비교용 구간(도보 제외, itinerary가 변환)
     * @param party      인원
     * @param overrides  사용자 설정. null이나 빈 목록이면 모든 상품을 공식가로
     * @return 원래 총액, 추천 총액, 추천 구매, 구간별 커버 상품. 에러 없음(상품이 없으면 원래 총액 그대로)
     */
    @Transactional(readOnly = true)
    public OptimizationResult execute(String regionCode, List<TicketLeg> legs, TicketParty party,
                                      List<ProductOverride> overrides) {
        Map<String, ProductOverride> overrideByCode = (overrides == null ? List.<ProductOverride>of() : overrides)
                .stream().collect(Collectors.toMap(ProductOverride::productCode, o -> o, (a, b) -> b));
        List<TicketProduct> products = productRepository.findByRegionCodeOrderByCode(regionCode).stream()
                .filter(p -> isIncluded(overrideByCode.get(p.getCode())))
                .toList();
        Map<String, List<Coverage>> coverageByCode = coverageRepository
                .findByProductCodeIn(products.stream().map(TicketProduct::getCode).toList()).stream()
                .collect(Collectors.groupingBy(TicketCoverage::getProductCode, Collectors.mapping(
                        c -> new Coverage(c.getOperatorCode(), c.getLineId(), c.getStationAId(), c.getStationBId()),
                        Collectors.toList())));
        List<ProductSpec> specs = products.stream()
                .map(p -> toSpec(p, coverageByCode.getOrDefault(p.getCode(), List.of()),
                        overrideByCode.get(p.getCode())))
                .toList();
        return optimizer.optimize(legs, specs, party);
    }

    // 설정이 없는 상품은 비교에 넣는다
    private static boolean isIncluded(ProductOverride override) {
        return override == null || override.included();
    }

    private static ProductSpec toSpec(TicketProduct p, List<Coverage> coverage, ProductOverride override) {
        int price = override != null && override.priceAmount() != null ? override.priceAmount() : p.getPrice();
        return new ProductSpec(p.getCode(), p.getType(), price, p.getValidDays(), p.getUses(),
                p.isCoversExpress(), p.getReservedSeatLimit(), coverage);
    }
}
