package io.github.jangws1030.creatorsettlementapi.settlement.application;

public record SettlementCalculationResult(
        long totalSaleAmount,
        long refundAmount,
        long netSaleAmount,
        long platformFeeAmount,
        long scheduledSettlementAmount,
        long saleCount,
        long cancellationCount,
        int feeRatePercentage
) {
}
