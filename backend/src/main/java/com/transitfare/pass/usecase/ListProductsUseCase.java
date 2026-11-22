package com.transitfare.pass.usecase;

import com.transitfare.pass.contract.ProductView;
import com.transitfare.pass.repository.TicketProductRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 사용자가 지역의 승차권 목록을 본다(여행 요약의 승차권 설정, 상품 안내). */
@Service
@RequiredArgsConstructor
public class ListProductsUseCase {

    private final TicketProductRepository productRepository;

    /**
     * 지역의 승차권 상품을 코드 순으로 돌려준다.
     *
     * @param regionCode 지역 코드(예: JP-FUKUOKA)
     * @return 상품 목록. 상품이 없는 지역이면 빈 목록(에러 없음)
     */
    @Transactional(readOnly = true)
    public List<ProductView> execute(String regionCode) {
        return productRepository.findByRegionCodeOrderByCode(regionCode).stream()
                .map(p -> new ProductView(p.getCode(), p.getType(), p.getNameKo(), p.getNameJa(),
                        p.getPrice(), p.getValidDays(), p.getEligibilityNote()))
                .toList();
    }
}
