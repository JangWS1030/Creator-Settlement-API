package io.github.jangws1030.creatorsettlementapi.sale.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public final class SaleApiDtos {

    private SaleApiDtos() {
    }

    public record CreateSaleRequest(
            String id,
            @NotBlank(message = "courseId is required.")
            String courseId,
            @NotBlank(message = "studentId is required.")
            String studentId,
            @Positive(message = "amount must be positive.")
            long amount,
            @NotNull(message = "paidAt is required.")
            OffsetDateTime paidAt
    ) {
    }

    public record CreateSaleResponse(
            String id,
            String creatorId,
            String creatorName,
            String courseId,
            String courseTitle,
            String studentId,
            long amount,
            OffsetDateTime paidAt
    ) {
    }

    public record CreateCancellationRequest(
            String id,
            @Positive(message = "refundAmount must be positive.")
            long refundAmount,
            @NotNull(message = "cancelledAt is required.")
            OffsetDateTime cancelledAt
    ) {
    }

    public record CreateCancellationResponse(
            String id,
            String saleId,
            long refundAmount,
            OffsetDateTime cancelledAt,
            long accumulatedRefundAmount,
            long remainingSaleAmount
    ) {
    }

    public record CancellationItemResponse(
            String id,
            long refundAmount,
            OffsetDateTime cancelledAt
    ) {
    }

    public record SaleItemResponse(
            String id,
            String courseId,
            String courseTitle,
            String studentId,
            long amount,
            OffsetDateTime paidAt,
            long refundedAmount,
            long remainingAmount,
            String saleStatus,
            List<CancellationItemResponse> cancellations
    ) {
    }

    public record SaleListResponse(
            String creatorId,
            String creatorName,
            LocalDate fromDate,
            LocalDate toDate,
            long totalSaleAmount,
            long totalRefundAmount,
            int itemCount,
            List<SaleItemResponse> items
    ) {
    }
}
