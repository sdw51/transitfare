package com.transitfare.pass.repository;

import com.transitfare.pass.entity.TicketCoverage;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 상품 적용 범위 저장소. */
public interface TicketCoverageRepository extends JpaRepository<TicketCoverage, Long> {

    /** 여러 상품의 적용 범위를 한 번에. */
    List<TicketCoverage> findByProductCodeIn(Collection<String> productCodes);
}
