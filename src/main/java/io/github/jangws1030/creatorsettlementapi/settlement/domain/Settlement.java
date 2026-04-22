package io.github.jangws1030.creatorsettlementapi.settlement.domain;

import io.github.jangws1030.creatorsettlementapi.creator.domain.Creator;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;

@Entity
@Table(
        name = "settlements",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_settlements_creator_year_month",
                        columnNames = {"creator_id", "settlement_month"}
                )
        },
        indexes = {
                @Index(name = "idx_settlements_creator_status", columnList = "creator_id, status"),
                @Index(name = "idx_settlements_year_month", columnList = "settlement_month")
        }
)
public class Settlement {

    @Id
    @Column(nullable = false, updatable = false, length = 100)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "creator_id", nullable = false)
    private Creator creator;

    @Column(name = "settlement_month", nullable = false, length = 7)
    private String yearMonth;

    @Column(nullable = false)
    private LocalDate periodStartDate;

    @Column(nullable = false)
    private LocalDate periodEndDate;

    @Column(nullable = false)
    private long totalSaleAmount;

    @Column(nullable = false)
    private long refundAmount;

    @Column(nullable = false)
    private long netSaleAmount;

    @Column(nullable = false)
    private long platformFeeAmount;

    @Column(nullable = false)
    private long scheduledSettlementAmount;

    @Column(nullable = false)
    private long saleCount;

    @Column(nullable = false)
    private long cancellationCount;

    @Column(nullable = false)
    private int feeRatePercentage;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SettlementStatus status;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    private OffsetDateTime confirmedAt;

    private OffsetDateTime paidAt;

    protected Settlement() {
    }

    public Settlement(
            Creator creator,
            YearMonth yearMonth,
            long totalSaleAmount,
            long refundAmount,
            long netSaleAmount,
            long platformFeeAmount,
            long scheduledSettlementAmount,
            long saleCount,
            long cancellationCount,
            int feeRatePercentage,
            OffsetDateTime createdAt
    ) {
        this.id = buildId(creator.getId(), yearMonth);
        this.creator = creator;
        this.yearMonth = yearMonth.toString();
        this.periodStartDate = yearMonth.atDay(1);
        this.periodEndDate = yearMonth.atEndOfMonth();
        this.totalSaleAmount = totalSaleAmount;
        this.refundAmount = refundAmount;
        this.netSaleAmount = netSaleAmount;
        this.platformFeeAmount = platformFeeAmount;
        this.scheduledSettlementAmount = scheduledSettlementAmount;
        this.saleCount = saleCount;
        this.cancellationCount = cancellationCount;
        this.feeRatePercentage = feeRatePercentage;
        this.status = SettlementStatus.PENDING;
        this.createdAt = createdAt;
    }

    public static String buildId(String creatorId, YearMonth yearMonth) {
        return "settlement-" + creatorId + "-" + yearMonth;
    }

    public void confirm(OffsetDateTime confirmedAt) {
        this.status = SettlementStatus.CONFIRMED;
        this.confirmedAt = confirmedAt;
    }

    public void markPaid(OffsetDateTime paidAt) {
        this.status = SettlementStatus.PAID;
        this.paidAt = paidAt;
    }

    public String getId() {
        return id;
    }

    public Creator getCreator() {
        return creator;
    }

    public String getYearMonth() {
        return yearMonth;
    }

    public LocalDate getPeriodStartDate() {
        return periodStartDate;
    }

    public LocalDate getPeriodEndDate() {
        return periodEndDate;
    }

    public long getTotalSaleAmount() {
        return totalSaleAmount;
    }

    public long getRefundAmount() {
        return refundAmount;
    }

    public long getNetSaleAmount() {
        return netSaleAmount;
    }

    public long getPlatformFeeAmount() {
        return platformFeeAmount;
    }

    public long getScheduledSettlementAmount() {
        return scheduledSettlementAmount;
    }

    public long getSaleCount() {
        return saleCount;
    }

    public long getCancellationCount() {
        return cancellationCount;
    }

    public int getFeeRatePercentage() {
        return feeRatePercentage;
    }

    public SettlementStatus getStatus() {
        return status;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public OffsetDateTime getConfirmedAt() {
        return confirmedAt;
    }

    public OffsetDateTime getPaidAt() {
        return paidAt;
    }
}
