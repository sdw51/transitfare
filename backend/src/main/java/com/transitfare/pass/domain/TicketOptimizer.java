package com.transitfare.pass.domain;

import com.transitfare.pass.contract.OptimizationResult;
import com.transitfare.pass.contract.ProductType;
import com.transitfare.pass.contract.Purchase;
import com.transitfare.pass.contract.TicketLeg;
import com.transitfare.pass.contract.TicketParty;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * 가장 싼 승차권 구매 조합을 찾는다(설계 문서 8장 "패스·할인 승차권 비교", 구현 계획 Task 7 알고리즘).
 * 어른 1명 기준으로 계산한 뒤 인원수를 곱한다.
 */
public class TicketOptimizer {

    static final int MAX_PURCHASES = 3;
    static final int MAX_CANDIDATES = 60;

    /**
     * 규칙: 후보 구매(PASS × 구간이 있는 각 날짜 시작 + FIXED_ROUTE × 1) 중 최대 3개 조합을 모두 따져
     * 총액(커버 안 된 요금 + 구매가)이 가장 싼 조합을 고른다. 동률이면 구매 수가 적은 조합.
     * 결과의 금액은 1명 기준 × 인원, 구매 수량 = 인원.
     */
    public OptimizationResult optimize(List<TicketLeg> legs, List<ProductSpec> products, TicketParty party) {
        int count = party.adults();
        if (count <= 0) {
            return new OptimizationResult(0, 0, List.of(), Map.of());
        }
        List<Unit> units = Unit.of(legs);
        Plan none = evaluate(legs, units, List.of());
        List<Candidate> candidates = candidates(legs, units, products, none.total());
        Plan best = search(legs, units, candidates, 0, new ArrayList<>(), none);
        List<Purchase> purchases = best.bought().stream()
                // 화면에 날짜순으로 보이도록 시작일 → 상품 코드 순(쓰이지 않은 구간 승차권은 시작일이 없어 맨 뒤)
                .sorted(Comparator.comparing(Bought::startDate, Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Bought::code))
                .map(b -> new Purchase(b.code(), b.startDate(), b.unitPrice(), count))
                .toList();
        return new OptimizationResult(none.total() * count, best.total() * count, purchases, best.coveredBy());
    }

    /**
     * 규칙: 아무 구간도 커버하지 못하는 후보는 버린다. 후보가 60개를 넘으면
     * 단독으로 샀을 때의 절감액(절감 − 가격)이 큰 60개만 남긴다.
     * 다일 패스는 여행 마지막 날을 넘겨도 되므로 시작일만 구간이 있는 날로 둔다.
     */
    private static List<Candidate> candidates(List<TicketLeg> legs, List<Unit> units, List<ProductSpec> products,
                                              int baseline) {
        List<LocalDate> dates = legs.stream().map(TicketLeg::date).distinct().sorted().toList();
        List<Candidate> all = new ArrayList<>();
        for (ProductSpec product : products) {
            if (product.type() == ProductType.PASS) {
                dates.forEach(date -> all.add(new Candidate(product, date)));
            } else {
                all.add(new Candidate(product, null));
            }
        }
        record Scored(Candidate candidate, int net) {
        }
        List<Scored> useful = all.stream()
                .map(c -> new Scored(c, baseline - evaluate(legs, units, List.of(c)).total()))
                // net + 가격 = 커버한 요금. 0이면 아무것도 커버하지 못한 후보
                .filter(s -> s.net() + s.candidate().price() > 0)
                .toList();
        if (useful.size() <= MAX_CANDIDATES) {
            return useful.stream().map(Scored::candidate).toList();
        }
        return useful.stream()
                .sorted(Comparator.comparingInt(Scored::net).reversed())
                .limit(MAX_CANDIDATES)
                .map(Scored::candidate)
                .toList();
    }

    /** 규칙: 후보 중 1~3개를 고르는 모든 조합을 차례로 평가해서 더 나은 계획을 남긴다. */
    private static Plan search(List<TicketLeg> legs, List<Unit> units, List<Candidate> candidates, int from,
                               List<Candidate> chosen, Plan best) {
        for (int i = from; i < candidates.size(); i++) {
            chosen.add(candidates.get(i));
            Plan plan = evaluate(legs, units, chosen);
            if (plan.isBetterThan(best)) {
                best = plan;
            }
            if (chosen.size() < MAX_PURCHASES) {
                best = search(legs, units, candidates, i + 1, chosen, best);
            }
            chosen.removeLast();
        }
        return best;
    }

    /**
     * 규칙: 한 조합 안에서 구간 덩어리를 날짜순으로 보며, 가장 크게 절감하는 구매에 배정한다.
     * 날짜순이어야 FIXED_ROUTE의 첫 사용일과 횟수, 지정석 횟수가 앞 구간부터 쓰인다.
     */
    private static Plan evaluate(List<TicketLeg> legs, List<Unit> units, List<Candidate> combo) {
        List<Usage> usages = combo.stream().map(Usage::new).toList();
        int total = combo.stream().mapToInt(Candidate::price).sum();
        Map<Integer, String> coveredBy = new TreeMap<>();
        for (Unit unit : units) {
            Usage chosen = null;
            int bestSaving = 0;
            for (Usage usage : usages) {
                int saving = usage.saving(legs, unit);
                if (saving > bestSaving) {
                    chosen = usage;
                    bestSaving = saving;
                }
            }
            total += unit.fare(legs) - bestSaving;
            if (chosen != null) {
                chosen.apply(legs, unit);
                for (int i : unit.legIndexes()) {
                    coveredBy.put(i, chosen.code());
                }
            }
        }
        return new Plan(total, usages.stream().map(Usage::toBought).toList(), coveredBy);
    }

    /** 연속 거리제 묶음(같은 groupId)은 묶음 전체가 커버될 때만 절감하므로 한 덩어리로 다룬다. */
    private record Unit(List<Integer> legIndexes, LocalDate date) {

        int size() {
            return legIndexes.size();
        }

        /** 덩어리의 1명 요금 = 구간마다 운임 + 특급요금 + 지정석 요금의 합. */
        int fare(List<TicketLeg> legs) {
            return legIndexes.stream().map(legs::get)
                    .mapToInt(l -> l.base() + l.expressFare() + l.reservedFare())
                    .sum();
        }

        /** 같은 groupId끼리 묶고 나머지는 구간 하나씩. 덩어리 날짜는 가장 이른 구간 날짜, 날짜·순번 순으로 정렬. */
        static List<Unit> of(List<TicketLeg> legs) {
            Map<String, List<Integer>> groups = new LinkedHashMap<>();
            for (int i = 0; i < legs.size(); i++) {
                Integer groupId = legs.get(i).groupId();
                String key = groupId != null ? "group-" + groupId : "leg-" + i;
                groups.computeIfAbsent(key, k -> new ArrayList<>()).add(i);
            }
            return groups.values().stream()
                    .map(idx -> new Unit(List.copyOf(idx),
                            idx.stream().map(i -> legs.get(i).date()).min(Comparator.naturalOrder()).orElseThrow()))
                    .sorted(Comparator.comparing(Unit::date).thenComparing(u -> u.legIndexes().getFirst()))
                    .toList();
        }
    }

    /** 후보 구매. start: PASS 시작일, FIXED_ROUTE는 null(첫 사용일에 정해짐). */
    private record Candidate(ProductSpec product, LocalDate start) {

        int price() {
            return product.price();
        }
    }

    /** 한 조합을 평가하는 동안 구매 하나의 사용 상태. */
    private static final class Usage {

        private final Candidate candidate;
        private LocalDate start;
        private int usesLeft;
        private int reservedLeft;

        Usage(Candidate candidate) {
            this.candidate = candidate;
            this.start = candidate.start();
            this.usesLeft = candidate.product().uses();
            this.reservedLeft = candidate.product().reservedSeatLimit();
        }

        String code() {
            return candidate.product().code();
        }

        /**
         * 규칙: 덩어리의 모든 구간이 요금 확인됨 + 적용 범위 + 유효 기간 안이어야 커버한다(하나라도 아니면 0).
         * 절감 = 운임 전액 + (특급 포함이면 특급요금) + (지정석은 남은 횟수만큼).
         * FIXED_ROUTE는 남은 승차 횟수가 덩어리 구간 수 이상일 때만 커버하고, 유효 기간은 첫 사용일부터.
         */
        int saving(List<TicketLeg> legs, Unit unit) {
            ProductSpec product = candidate.product();
            if (product.type() == ProductType.FIXED_ROUTE && usesLeft < unit.size()) {
                return 0;
            }
            // FIXED_ROUTE는 아직 안 썼으면 이 덩어리 날짜가 첫 사용일이 된다
            LocalDate from = start != null ? start : unit.date();
            LocalDate to = from.plusDays(product.validDays() - 1L);
            int saving = 0;
            int reservedRemaining = reservedLeft;
            for (int i : unit.legIndexes()) {
                TicketLeg leg = legs.get(i);
                if (!leg.fareKnown() || !product.covers(leg)
                        || leg.date().isBefore(from) || leg.date().isAfter(to)) {
                    return 0;
                }
                saving += leg.base();
                if (product.coversExpress()) {
                    saving += leg.expressFare();
                }
                if (leg.reserved() && reservedRemaining > 0) {
                    saving += leg.reservedFare();
                    reservedRemaining--;
                }
            }
            return saving;
        }

        /** 배정이 정해진 뒤 첫 사용일·남은 승차 횟수·남은 지정석 횟수를 줄인다. PASS의 횟수는 쓰지 않는다. */
        void apply(List<TicketLeg> legs, Unit unit) {
            if (start == null) {
                start = unit.date();
            }
            usesLeft -= unit.size();
            for (int i : unit.legIndexes()) {
                if (legs.get(i).reserved() && reservedLeft > 0) {
                    reservedLeft--;
                }
            }
        }

        Bought toBought() {
            return new Bought(code(), start, candidate.price());
        }
    }

    private record Bought(String code, LocalDate startDate, int unitPrice) {
    }

    private record Plan(int total, List<Bought> bought, Map<Integer, String> coveredBy) {

        /** 더 싸면 낫고, 같으면 구매 수가 적은 쪽이 낫다. */
        boolean isBetterThan(Plan other) {
            return total < other.total || (total == other.total && bought.size() < other.bought.size());
        }
    }
}
