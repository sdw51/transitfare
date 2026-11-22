package com.transitfare.transit.repository;

import com.transitfare.transit.entity.ReservedSeatFee;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

/** 지정석 요금 */
public interface ReservedSeatFeeRepository extends JpaRepository<ReservedSeatFee, Long> {

    Optional<ReservedSeatFee> findByOperatorCode(String operatorCode);
}
