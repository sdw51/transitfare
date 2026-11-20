package com.transitfare.pass.contract;

import java.util.List;
import java.util.Map;

/** legCoveredBy: TicketLeg 순번 → productCode */
public record OptimizationResult(int baselineTotal, int optimizedTotal, List<Purchase> purchases,
                                 Map<Integer, String> legCoveredBy) {
}
