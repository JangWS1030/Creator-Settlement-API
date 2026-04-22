package io.github.jangws1030.creatorsettlementapi.sale.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(
        name = "cancellation_records",
        indexes = {
                @Index(name = "idx_cancellation_records_sale_cancelled_at", columnList = "sale_record_id, cancelled_at"),
                @Index(name = "idx_cancellation_records_cancelled_at", columnList = "cancelled_at")
        }
)
public class CancellationRecord {

    @Id
    @Column(nullable = false, updatable = false, length = 50)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sale_record_id", nullable = false)
    private SaleRecord saleRecord;

    @Column(nullable = false)
    private long refundAmount;

    @Column(nullable = false)
    private OffsetDateTime cancelledAt;

    protected CancellationRecord() {
    }

    public CancellationRecord(
            String id,
            SaleRecord saleRecord,
            long refundAmount,
            OffsetDateTime cancelledAt
    ) {
        this.id = id;
        this.saleRecord = saleRecord;
        this.refundAmount = refundAmount;
        this.cancelledAt = cancelledAt;
    }

    public void revise(SaleRecord saleRecord, long refundAmount, OffsetDateTime cancelledAt) {
        this.saleRecord = saleRecord;
        this.refundAmount = refundAmount;
        this.cancelledAt = cancelledAt;
    }

    public String getId() {
        return id;
    }

    public SaleRecord getSaleRecord() {
        return saleRecord;
    }

    public long getRefundAmount() {
        return refundAmount;
    }

    public OffsetDateTime getCancelledAt() {
        return cancelledAt;
    }
}
