package com.transitfare.transit.domain;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;

/** FareEngineTest용 메모리 FareData. with…로 가상 데이터를 쌓는다(실제 요금 아님). */
class InMemoryFareData implements FareData {

    private final Map<String, OperatorRule> operators = new HashMap<>();
    /** "노선:역" → 누적 km */
    private final Map<String, BigDecimal> kms = new HashMap<>();
    /** "운영사:종류" → (max_km → 금액) */
    private final Map<String, TreeMap<BigDecimal, Integer>> bands = new HashMap<>();
    /** "운영사:종류:작은id:큰id" → 금액 */
    private final Map<String, Integer> pairs = new HashMap<>();
    private final List<Zone> zones = new ArrayList<>();
    private final Map<String, Integer> reservedFees = new HashMap<>();
    /** "from>to" → 할인 */
    private final Map<String, Integer> discounts = new HashMap<>();

    private record Zone(String operatorCode, int amount, Set<Long> stations) {
    }

    InMemoryFareData withOperator(String code, FareRuleType type) {
        operators.put(code, new OperatorRule(code, type));
        return this;
    }

    InMemoryFareData withKm(long lineId, long stationId, String km) {
        kms.put(lineId + ":" + stationId, new BigDecimal(km));
        return this;
    }

    InMemoryFareData withBand(String op, String kind, String maxKm, int amount) {
        bands.computeIfAbsent(op + ":" + kind, k -> new TreeMap<>()).put(new BigDecimal(maxKm), amount);
        return this;
    }

    InMemoryFareData withPair(String op, String kind, long a, long b, int amount) {
        pairs.put(pairKey(op, kind, a, b), amount);
        return this;
    }

    InMemoryFareData withZone(String op, int amount, Long... stations) {
        zones.add(new Zone(op, amount, Set.of(stations)));
        return this;
    }

    InMemoryFareData withReservedFee(String op, int amount) {
        reservedFees.put(op, amount);
        return this;
    }

    InMemoryFareData withDiscount(String from, String to, int amount) {
        discounts.put(from + ">" + to, amount);
        return this;
    }

    @Override
    public Optional<OperatorRule> operator(String code) {
        return Optional.ofNullable(operators.get(code));
    }

    @Override
    public Optional<BigDecimal> cumulativeKm(long lineId, long stationId) {
        return Optional.ofNullable(kms.get(lineId + ":" + stationId));
    }

    @Override
    public Optional<Integer> distanceBandFare(String op, String kind, BigDecimal km) {
        TreeMap<BigDecimal, Integer> table = bands.get(op + ":" + kind);
        // km 이상인 가장 작은 밴드
        return table == null ? Optional.empty() : Optional.ofNullable(table.ceilingEntry(km)).map(Map.Entry::getValue);
    }

    @Override
    public Optional<Integer> pairFare(String op, String kind, long a, long b) {
        return Optional.ofNullable(pairs.get(pairKey(op, kind, a, b)));
    }

    @Override
    public Optional<Integer> flatZoneFare(String op, long a, long b) {
        return zones.stream()
                .filter(z -> z.operatorCode().equals(op) && z.stations().contains(a) && z.stations().contains(b))
                .findFirst()
                .map(Zone::amount);
    }

    @Override
    public Optional<Integer> reservedSeatFee(String op) {
        return Optional.ofNullable(reservedFees.get(op));
    }

    @Override
    public Optional<Integer> transferDiscount(String from, String to) {
        return Optional.ofNullable(discounts.get(from + ">" + to));
    }

    /** 양방향 동일: 작은 id를 앞에 둬서 (a,b)와 (b,a)가 같은 키 */
    private static String pairKey(String op, String kind, long a, long b) {
        return op + ":" + kind + ":" + Math.min(a, b) + ":" + Math.max(a, b);
    }
}
