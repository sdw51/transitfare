package com.transitfare.pass.controller;

import com.transitfare.pass.PassFacade;
import com.transitfare.pass.contract.ProductView;
import com.transitfare.pass.usecase.ListProductsUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 승차권 상품 조회 API. 조회만 하고 저장하지 않는다.
 * 프론트의 승차권 안내·설정 화면이 부른다. 다른 모듈(itinerary)은 HTTP가 아니라 {@link PassFacade}로 부른다.
 */
@Tag(name = "승차권", description = "패스·할인 승차권 상품")
@RestController
@RequestMapping("/api/v1/products")
@RequiredArgsConstructor
public class ProductController {

    private final PassFacade passFacade;

    /**
     * 지역의 승차권 상품 목록. type은 {code, label, description}.
     * <p>언제: 사용자가 여행 화면에서 승차권 목록(이름·가격·이용 조건)을 볼 때.
     * <p>{@link PassFacade#listProducts} → {@link ListProductsUseCase}: 지역 상품을 코드 순으로 읽는다.
     * region이 없으면 400 INVALID_INPUT(공통 처리), 상품이 없는 지역이면 빈 목록.
     */
    @Operation(summary = "지역의 승차권 상품 목록")
    @GetMapping
    public List<ProductView> listProducts(@RequestParam("region") String region) {
        return passFacade.listProducts(region);
    }
}
