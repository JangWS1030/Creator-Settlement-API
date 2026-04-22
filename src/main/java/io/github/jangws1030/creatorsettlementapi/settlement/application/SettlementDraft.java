package io.github.jangws1030.creatorsettlementapi.settlement.application;

import io.github.jangws1030.creatorsettlementapi.creator.domain.Creator;
import java.time.YearMonth;

public record SettlementDraft(
        Creator creator,
        YearMonth yearMonth,
        SettlementCalculationResult calculationResult
) {
}
