package com.transitfare.transit.repository;

import com.transitfare.transit.entity.TransferDiscount;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 환승 할인 */
public interface TransferDiscountRepository extends JpaRepository<TransferDiscount, Long> {

    Optional<TransferDiscount> findByFromOperatorCodeAndToOperatorCode(String fromOperatorCode, String toOperatorCode);
}
