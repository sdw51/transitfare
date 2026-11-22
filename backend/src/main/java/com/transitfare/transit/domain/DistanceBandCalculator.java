package com.transitfare.transit.domain;

import com.transitfare.transit.contract.LegInput;
import com.transitfare.transit.contract.LegMode;
import com.transitfare.transit.contract.SeatType;
import com.transitfare.transit.contract.TrainClass;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * 거리 구간제(설계 7-1: 후쿠오카 지하철, JR 큐슈). 묶음(같은 운영사 연속 구간) 하나를 계산한다.
 * 운임은 묶음 전체로 한 번 구해 첫 구간에 넣는다. 특급요금·지정석 요금은 열차(앞 구간과 같은 열차로 이어진 구간들)마다
 * 한 번 구해 그 열차의 첫 구간에 넣는다.
 */
class DistanceBandCalculator {

    static final String NOTE_GROUPED = "앞 구간과 합산";

    private final FareData data;

    DistanceBandCalculator(FareData data) {
        this.data = data;
    }

    /**
     * 묶음 하나 계산.
     *
     * @param legs    묶음 구간들(사이 도보 포함, 입력 순서 그대로)
     * @param groupId 묶음 번호(탄 구간이 하나뿐이면 null)
     * @return legs와 같은 순서·개수의 요금. 도보는 0원. 하나라도 모르면 탄 구간 전부 UNKNOWN
     */
    List<LegCharge> calculate(List<LegInput> legs, Integer groupId) {
        List<LegInput> rides = legs.stream().filter(l -> l.mode() == LegMode.TRANSIT).toList();
        String operator = rides.get(0).operatorCode();

        // 묶음 전체 거리. 한 구간이라도 거리를 모르면 요금을 구할 수 없다
        Optional<BigDecimal> km = totalKm(rides);
        if (km.isEmpty()) {
            return spread(legs, groupId, LegCharge.missing(List.of("거리")));
        }

        // 운임: 묶음 전체로 한 번. 첫 승차역~마지막 하차역 쌍 요금(특정 구간 예외)이 있으면 그것, 없으면 거리 밴드
        Optional<Integer> base = pairOrBand(operator, FareData.BASE,
                rides.get(0).fromStationId(), rides.get(rides.size() - 1).toStationId(), km.get());

        // 특급요금: 열차마다 따로(같은 열차로 노선만 바뀌면 한 열차, 다른 특급으로 갈아타면 열차마다)
        List<List<LegInput>> trains = splitTrains(rides);
        List<Optional<Integer>> expressByTrain = trains.stream().map(t -> expressFare(operator, t)).toList();
        Optional<Integer> reservedFee = anyReserved(rides) ? data.reservedSeatFee(operator) : Optional.of(0);

        List<String> missing = new ArrayList<>();
        if (base.isEmpty()) {
            missing.add("운임");
        }
        if (expressByTrain.stream().anyMatch(Optional::isEmpty)) {
            missing.add("특급요금");
        }
        if (reservedFee.isEmpty()) {
            missing.add("지정석 요금");
        }
        if (!missing.isEmpty()) {
            return spread(legs, groupId, LegCharge.missing(missing));
        }
        List<TrainFee> feeByRide = trainFeeByRide(trains, expressByTrain, reservedFee.get());
        return distribute(legs, groupId, base.get(), feeByRide);
    }

    /** 열차 나누기: 앞 구간과 같은 열차(continuesPreviousTrain)면 이어 붙이고, 아니면 새 열차 */
    private static List<List<LegInput>> splitTrains(List<LegInput> rides) {
        List<List<LegInput>> trains = new ArrayList<>();
        for (LegInput ride : rides) {
            if (trains.isEmpty() || !ride.continuesPreviousTrain()) {
                trains.add(new ArrayList<>());
            }
            trains.get(trains.size() - 1).add(ride);
        }
        return trains;
    }

    /** 탄 구간 하나에 붙는 열차 요금(특급요금·지정석 요금) */
    private record TrainFee(int express, int reserved) {
    }

    /**
     * 열차 요금을 탄 구간 순서대로 펼친다. 열차의 첫 구간에 그 열차의 특급요금과(지정석 구간이 있으면) 지정석 요금 1회,
     * 이어 타는 구간은 0.
     */
    private static List<TrainFee> trainFeeByRide(List<List<LegInput>> trains, List<Optional<Integer>> expressByTrain,
                                                 int reservedFee) {
        List<TrainFee> result = new ArrayList<>();
        for (int t = 0; t < trains.size(); t++) {
            List<LegInput> train = trains.get(t);
            int reserved = anyReserved(train) ? reservedFee : 0;
            result.add(new TrainFee(expressByTrain.get(t).get(), reserved));
            for (int k = 1; k < train.size(); k++) {
                result.add(new TrainFee(0, 0));
            }
        }
        return result;
    }

    /**
     * 구한 요금을 구간에 나눠 담는다. 운임은 묶음 첫 탄 구간, 나머지 탄 구간은 0 + "앞 구간과 합산".
     * 특급요금·지정석 요금은 feeByRide(탄 구간 순서)대로. 도보는 0원.
     */
    private List<LegCharge> distribute(List<LegInput> legs, Integer groupId, int base, List<TrainFee> feeByRide) {
        List<LegCharge> result = new ArrayList<>();
        int rideIndex = 0;
        for (LegInput leg : legs) {
            if (leg.mode() == LegMode.WALK) {
                result.add(LegCharge.free());
                continue;
            }
            TrainFee fee = feeByRide.get(rideIndex);
            LegCharge charge = rideIndex == 0
                    ? LegCharge.ok(base, fee.express(), fee.reserved(), null)
                    : LegCharge.ok(0, fee.express(), fee.reserved(), NOTE_GROUPED);
            result.add(charge.inGroup(groupId));
            rideIndex++;
        }
        return result;
    }

    /** 미확인일 때: 탄 구간은 모두 같은 미확인 요금, 도보는 0원 */
    private List<LegCharge> spread(List<LegInput> legs, Integer groupId, LegCharge unknown) {
        return legs.stream()
                .map(l -> l.mode() == LegMode.WALK ? LegCharge.free() : unknown.inGroup(groupId))
                .toList();
    }

    /**
     * 열차 하나의 특급요금: 특급 구간이 없으면 0. 있으면 첫 특급 승차역~마지막 특급 하차역 쌍 요금,
     * 없으면 특급 구간 거리 합의 밴드
     */
    private Optional<Integer> expressFare(String operator, List<LegInput> train) {
        List<LegInput> expressRides = train.stream().filter(l -> l.trainClass() == TrainClass.EXPRESS).toList();
        if (expressRides.isEmpty()) {
            return Optional.of(0);
        }
        return totalKm(expressRides).flatMap(km -> pairOrBand(operator, FareData.EXPRESS,
                expressRides.get(0).fromStationId(), expressRides.get(expressRides.size() - 1).toStationId(), km));
    }

    /** 쌍 요금이 있으면 우선(특정 구간 예외, 사용자 결정), 없으면 거리 밴드 */
    private Optional<Integer> pairOrBand(String operator, String kind, long from, long to, BigDecimal km) {
        return data.pairFare(operator, kind, from, to)
                .or(() -> data.distanceBandFare(operator, kind, km));
    }

    /** 구간 거리의 합. 한 구간이라도 거리를 모르면 empty */
    private Optional<BigDecimal> totalKm(List<LegInput> rides) {
        BigDecimal sum = BigDecimal.ZERO;
        for (LegInput ride : rides) {
            Optional<BigDecimal> km = legKm(ride);
            if (km.isEmpty()) {
                return Optional.empty();
            }
            sum = sum.add(km.get());
        }
        return Optional.of(sum);
    }

    /** 구간 거리 = |하차역 누적 km − 승차역 누적 km| (같은 노선 위). 노선·역·누적 km 중 하나라도 없으면 empty */
    private Optional<BigDecimal> legKm(LegInput leg) {
        if (leg.lineId() == null || leg.fromStationId() == null || leg.toStationId() == null) {
            return Optional.empty();
        }
        Optional<BigDecimal> from = data.cumulativeKm(leg.lineId(), leg.fromStationId());
        Optional<BigDecimal> to = data.cumulativeKm(leg.lineId(), leg.toStationId());
        if (from.isEmpty() || to.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(to.get().subtract(from.get()).abs());
    }

    private static boolean anyReserved(List<LegInput> rides) {
        return rides.stream().anyMatch(l -> l.seatType() == SeatType.RESERVED);
    }
}
