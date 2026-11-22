package com.transitfare.pass;

import com.transitfare.pass.contract.OptimizationResult;
import com.transitfare.pass.contract.ProductOverride;
import com.transitfare.pass.contract.ProductView;
import com.transitfare.pass.contract.TicketLeg;
import com.transitfare.pass.contract.TicketParty;
import com.transitfare.pass.usecase.ListProductsUseCase;
import com.transitfare.pass.usecase.OptimizeTicketsUseCase;
import java.util.List;

/** pass 모듈의 공개 진입점. itinerary(여행 요약)와 pass 컨트롤러가 부른다. transit을 모른다. */
public interface PassFacade {

    /**
     * 지역의 승차권 상품 목록.
     * {@link ListProductsUseCase}: 지역의 상품을 코드 순으로 읽어 ProductView로 돌려준다.
     */
    List<ProductView> listProducts(String regionCode);

    /**
     * 구간 목록에 대해 가장 싼 승차권 구매 조합을 계산한다.
     * {@link OptimizeTicketsUseCase}: 지역 상품에 사용자 설정(제외·가격 변경)을 적용하고,
     * 최대 3개 구매 조합 중 총액(커버 안 된 요금 + 구매가)이 가장 싼 것을 고른다.
     */
    OptimizationResult optimize(String regionCode, List<TicketLeg> legs, TicketParty party,
                                List<ProductOverride> overrides);
}
