package com.transitfare.transit.repository;

import com.transitfare.transit.domain.FareData;
import com.transitfare.transit.domain.OperatorRule;
import com.transitfare.transit.entity.DistanceFareBand;
import com.transitfare.transit.entity.FlatZone;
import com.transitfare.transit.entity.LineStation;
import com.transitfare.transit.entity.LineStationId;
import com.transitfare.transit.entity.PairFare;
import com.transitfare.transit.entity.ReservedSeatFee;
import com.transitfare.transit.entity.TransferDiscount;
import java.math.BigDecimal;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * FareData 포트의 JPA 구현. 요금 계산 엔진이 읽는 값을 transit 스키마의 요금 테이블에서 꺼내 준다(읽기 전용).
 * 엔티티는 밖으로 내보내지 않고 금액·km 같은 값만 돌려준다. 값이 없으면 Optional.empty(엔진이 "미확인"으로 처리).
 */
@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JpaFareData implements FareData {

    private final OperatorRepository operatorRepository;
    private final LineStationRepository lineStationRepository;
    private final DistanceFareBandRepository distanceFareBandRepository;
    private final PairFareRepository pairFareRepository;
    private final FlatZoneRepository flatZoneRepository;
    private final ReservedSeatFeeRepository reservedSeatFeeRepository;
    private final TransferDiscountRepository transferDiscountRepository;

    /**
     * 운영사 코드로 요금 계산 방식을 찾는다(operator 테이블).
     *
     * @param code 운영사 코드(예: JR_KYUSHU)
     * @return 코드와 요금 계산 방식. 등록되지 않은 운영사면 empty
     */
    @Override
    public Optional<OperatorRule> operator(String code) {
        return operatorRepository.findById(code)
                .map(o -> new OperatorRule(o.getCode(), o.getFareRuleType()));
    }

    /**
     * 노선 기점부터 역까지의 누적 km(line_station 테이블). 엔진은 두 역의 누적 km 차이로 구간 거리를 구한다.
     *
     * @param lineId    노선 id
     * @param stationId 역 id
     * @return 누적 km. 그 역이 이 노선에 없으면 empty
     */
    @Override
    public Optional<BigDecimal> cumulativeKm(long lineId, long stationId) {
        return lineStationRepository.findById(new LineStationId(lineId, stationId))
                .map(LineStation::getCumulativeKm);
    }

    /**
     * 거리 구간 요금(distance_fare_band 테이블). km 이상인 밴드 중 max_km가 가장 작은 밴드의 금액.
     * 예: 밴드 3.0→210, 7.0→260이면 3.0km→210, 3.1km→260.
     *
     * @param operatorCode 운영사 코드
     * @param kind         BASE(운임) 또는 EXPRESS(특급요금)
     * @param km           구간 거리(묶음이면 합한 거리)
     * @return 금액. km가 가장 큰 밴드보다 멀면 empty
     */
    @Override
    public Optional<Integer> distanceBandFare(String operatorCode, String kind, BigDecimal km) {
        return distanceFareBandRepository
                .findFirstByOperatorCodeAndKindAndMaxKmGreaterThanEqualOrderByMaxKmAsc(operatorCode, kind, km)
                .map(DistanceFareBand::getAmount);
    }

    /**
     * 역 쌍 요금(pair_fare 테이블). 양방향 동일이라 (a, b)와 (b, a) 어느 쪽으로 저장돼 있어도 찾는다.
     * 정류장 간 요금표 운영사의 기본 요금이고, 거리제 운영사(JR 등)에서는 밴드보다 먼저 보는 특정 구간 예외다.
     *
     * @param operatorCode 운영사 코드
     * @param kind         BASE(운임) 또는 EXPRESS(특급요금)
     * @param a            한쪽 역 id
     * @param b            다른 쪽 역 id
     * @return 금액. 이 쌍의 요금이 없으면 empty
     */
    @Override
    public Optional<Integer> pairFare(String operatorCode, String kind, long a, long b) {
        return pairFareRepository.findPair(operatorCode, kind, a, b)
                .map(PairFare::getAmount);
    }

    /**
     * 균일 구간 요금(flat_zone, flat_zone_station 테이블). 두 정류장이 모두 같은 균일 구간에 있을 때만 값이 있다.
     *
     * @param operatorCode 운영사 코드
     * @param a            승차 정류장 id
     * @param b            하차 정류장 id
     * @return 균일 구간 요금. 한쪽이라도 구간 밖이면 empty(엔진은 역 쌍 요금으로 넘어간다)
     */
    @Override
    public Optional<Integer> flatZoneFare(String operatorCode, long a, long b) {
        // 균일 구간이 겹쳐 두 개 이상 맞으면 id가 가장 작은(먼저 등록된) 구간의 요금
        return flatZoneRepository.findZonesContainingBoth(operatorCode, a, b).stream()
                .findFirst()
                .map(FlatZone::getAmount);
    }

    /**
     * 지정석 요금(reserved_seat_fee 테이블, 1회 승차당).
     *
     * @param operatorCode 운영사 코드
     * @return 금액. 지정석 요금이 등록되지 않은 운영사면 empty
     */
    @Override
    public Optional<Integer> reservedSeatFee(String operatorCode) {
        return reservedSeatFeeRepository.findByOperatorCode(operatorCode)
                .map(ReservedSeatFee::getAmount);
    }

    /**
     * 회사 간 환승 할인(transfer_discount 테이블). 방향이 있다: from → to로 갈아탈 때만 적용.
     *
     * @param fromOperator 먼저 탄 운영사 코드
     * @param toOperator   갈아탄 운영사 코드
     * @return 두 번째 구간 운임에서 뺄 금액. 할인이 없으면 empty
     */
    @Override
    public Optional<Integer> transferDiscount(String fromOperator, String toOperator) {
        return transferDiscountRepository.findByFromOperatorCodeAndToOperatorCode(fromOperator, toOperator)
                .map(TransferDiscount::getDiscount);
    }
}
