package com.transitfare.pass.repository;

import com.transitfare.pass.entity.TicketProduct;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 승차권 상품 저장소. */
public interface TicketProductRepository extends JpaRepository<TicketProduct, String> {

    /** 지역의 상품을 코드 순으로. */
    List<TicketProduct> findByRegionCodeOrderByCode(String regionCode);
}
