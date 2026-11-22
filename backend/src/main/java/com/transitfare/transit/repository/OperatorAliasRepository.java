package com.transitfare.transit.repository;

import com.transitfare.transit.entity.OperatorAlias;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** 운영사 다른 이름 (역 매칭 Task 10-A에서 사용) */
public interface OperatorAliasRepository extends JpaRepository<OperatorAlias, Long> {

    /** 지역 안에서 별칭이 정확히 같은 운영사 코드들(코드 순) */
    @Query("""
            select a.operatorCode from OperatorAlias a, Operator o
            where o.code = a.operatorCode and a.alias = :alias and o.regionCode = :regionCode
            order by a.operatorCode
            """)
    List<String> findOperatorCodes(String regionCode, String alias);
}
