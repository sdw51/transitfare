package com.transitfare.transit.repository;

import com.transitfare.transit.entity.Operator;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** 운영사 */
public interface OperatorRepository extends JpaRepository<Operator, String> {

    /** 지역의 운영사 목록(코드 순) */
    List<Operator> findByRegionCodeOrderByCode(String regionCode);
}
