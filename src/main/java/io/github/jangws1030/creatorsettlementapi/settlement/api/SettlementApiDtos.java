package io.github.jangws1030.creatorsettlementapi.settlement.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class SettlementApiDtos {

    private SettlementApiDtos() {
    }

    public record MonthlySettlementResponse(
            String creatorId,
            String creatorName,
            String yearMonth,
            long totalSaleAmount,
            long refundAmount,
            long netSaleAmount,
            long platformFeeAmount,
            long scheduledSettlementAmount,
            long saleCount,
            long cancellationCount,
            int feeRatePercentage,
            String settlementId,
            String settlementStatus
    ) {
    }

    public record AdminCreatorSettlementResponse(
            String creatorId,
            String creatorName,
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

    public record AdminSettlementResponse(
            LocalDate startDate,
            LocalDate endDate,
            long totalSaleAmount,
            long totalRefundAmount,
            long totalNetSaleAmount,
            long totalPlatformFeeAmount,
            long totalScheduledSettlementAmount,
            List<AdminCreatorSettlementResponse> creatorSettlements
    ) {
    }

    public record CreateSettlementRequest(
            @NotBlank(message = "creatorId is required.")
            String creatorId,
            @NotBlank(message = "yearMonth is required.")
            String yearMonth
    ) {
    }

    public record SettlementResponse(
            String id,
            String creatorId,
            String creatorName,
            String yearMonth,
            LocalDate periodStartDate,
            LocalDate periodEndDate,
            long totalSaleAmount,
            long refundAmount,
            long netSaleAmount,
            long platformFeeAmount,
            long scheduledSettlementAmount,
            long saleCount,
            long cancellationCount,
            int feeRatePercentage,
            String status,
            OffsetDateTime createdAt,
            OffsetDateTime confirmedAt,
            OffsetDateTime paidAt
    ) {
    }

    public record CreateFeeRateRequest(
            @NotNull(message = "effectiveFrom is required.")
            LocalDate effectiveFrom,
            @NotNull(message = "feeRatePercentage is required.")
            @Min(value = 0, message = "feeRatePercentage must be between 0 and 100.")
            @Max(value = 100, message = "feeRatePercentage must be between 0 and 100.")
            Integer feeRatePercentage
    ) {
    }

    public record FeeRateResponse(
            Long id,
            LocalDate effectiveFrom,
            int feeRatePercentage,
            OffsetDateTime createdAt
    ) {
    }
}
