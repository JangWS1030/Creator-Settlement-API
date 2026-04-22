package io.github.jangws1030.creatorsettlementapi.settlement.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import org.springframework.stereotype.Component;

@Component
public class SettlementCalculator {

    public long calculatePlatformFeeAmount(long amount, int feeRatePercentage) {
        return BigDecimal.valueOf(amount)
                .multiply(BigDecimal.valueOf(feeRatePercentage))
                .divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP)
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }

    public SettlementCalculationResult calculate(
            long totalSaleAmount,
            long refundAmount,
            long saleCount,
            long cancellationCount,
            int feeRatePercentage
    ) {
        long netSaleAmount = totalSaleAmount - refundAmount;
        long platformFeeAmount = calculatePlatformFeeAmount(netSaleAmount, feeRatePercentage);
        long scheduledSettlementAmount = netSaleAmount - platformFeeAmount;

        return new SettlementCalculationResult(
                totalSaleAmount,
                refundAmount,
                netSaleAmount,
                platformFeeAmount,
                scheduledSettlementAmount,
                saleCount,
                cancellationCount,
                feeRatePercentage
        );
    }
}
