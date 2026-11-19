package com.transitfare.transit.repository;

import com.transitfare.transit.entity.OperatorAlias;
import org.springframework.data.jpa.repository.JpaRepository;

/** 운영사 다른 이름 (역 매칭 Task 10-A에서 사용) */
public interface OperatorAliasRepository extends JpaRepository<OperatorAlias, Long> {
}
