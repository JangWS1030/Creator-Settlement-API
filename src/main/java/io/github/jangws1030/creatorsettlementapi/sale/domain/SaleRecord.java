package io.github.jangws1030.creatorsettlementapi.sale.domain;

import io.github.jangws1030.creatorsettlementapi.course.domain.Course;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Table(
        name = "sale_records",
        indexes = {
                @Index(name = "idx_sale_records_course_paid_at", columnList = "course_id, paid_at"),
                @Index(name = "idx_sale_records_paid_at", columnList = "paid_at")
        }
)
public class SaleRecord {

    @Id
    @Column(nullable = false, updatable = false, length = 50)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "course_id", nullable = false)
    private Course course;

    @Column(nullable = false, length = 50)
    private String studentId;

    @Column(nullable = false)
    private long amount;

    @Column(nullable = false)
    private OffsetDateTime paidAt;

    @OrderBy("cancelledAt ASC, id ASC")
    @OneToMany(mappedBy = "saleRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CancellationRecord> cancellations = new ArrayList<>();

    protected SaleRecord() {
    }

    public SaleRecord(
            String id,
            Course course,
            String studentId,
            long amount,
            OffsetDateTime paidAt
    ) {
        this.id = id;
        this.course = course;
        this.studentId = studentId;
        this.amount = amount;
        this.paidAt = paidAt;
    }

    public void revise(Course course, String studentId, long amount, OffsetDateTime paidAt) {
        this.course = course;
        this.studentId = studentId;
        this.amount = amount;
        this.paidAt = paidAt;
    }

    public void addCancellation(CancellationRecord cancellationRecord) {
        cancellations.add(cancellationRecord);
    }

    public long getRefundedAmount() {
        return cancellations.stream()
                .mapToLong(CancellationRecord::getRefundAmount)
                .sum();
    }

    public String getId() {
        return id;
    }

    public Course getCourse() {
        return course;
    }

    public String getStudentId() {
        return studentId;
    }

    public long getAmount() {
        return amount;
    }

    public OffsetDateTime getPaidAt() {
        return paidAt;
    }

    public List<CancellationRecord> getCancellations() {
        return Collections.unmodifiableList(cancellations);
    }
}
