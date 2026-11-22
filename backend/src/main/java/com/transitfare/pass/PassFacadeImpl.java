package com.transitfare.pass;

import com.transitfare.pass.contract.OptimizationResult;
import com.transitfare.pass.contract.ProductOverride;
import com.transitfare.pass.contract.ProductView;
import com.transitfare.pass.contract.TicketLeg;
import com.transitfare.pass.contract.TicketParty;
import com.transitfare.pass.usecase.ListProductsUseCase;
import com.transitfare.pass.usecase.OptimizeTicketsUseCase;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** PassFacade 구현. 메서드마다 유스케이스 하나에 넘긴다. */
@Service
@RequiredArgsConstructor
public class PassFacadeImpl implements PassFacade {

    private final ListProductsUseCase listProductsUseCase;
    private final OptimizeTicketsUseCase optimizeTicketsUseCase;

    /** 지역 상품 목록. {@link ListProductsUseCase}: 지역 상품을 코드 순으로 읽는다. */
    @Override
    public List<ProductView> listProducts(String regionCode) {
        return listProductsUseCase.execute(regionCode);
    }

    /** 가장 싼 승차권 조합. {@link OptimizeTicketsUseCase}: 사용자 설정을 적용한 상품으로 TicketOptimizer를 돌린다. */
    @Override
    public OptimizationResult optimize(String regionCode, List<TicketLeg> legs, TicketParty party,
                                       List<ProductOverride> overrides) {
        return optimizeTicketsUseCase.execute(regionCode, legs, party, overrides);
    }
}
