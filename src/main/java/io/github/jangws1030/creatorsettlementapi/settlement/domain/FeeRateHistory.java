package io.github.jangws1030.creatorsettlementapi.settlement.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "fee_rate_histories",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_fee_rate_histories_effective_from", columnNames = "effective_from")
        }
)
public class FeeRateHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(nullable = false)
    private int feeRatePercentage;

    @Column(nullable = false)
    private OffsetDateTime createdAt;

    protected FeeRateHistory() {
    }

    public FeeRateHistory(LocalDate effectiveFrom, int feeRatePercentage, OffsetDateTime createdAt) {
        this.effectiveFrom = effectiveFrom;
        this.feeRatePercentage = feeRatePercentage;
        this.createdAt = createdAt;
    }

    public void revise(int feeRatePercentage, OffsetDateTime createdAt) {
        this.feeRatePercentage = feeRatePercentage;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public int getFeeRatePercentage() {
        return feeRatePercentage;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }
}
