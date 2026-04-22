package io.github.jangws1030.creatorsettlementapi.sale.repository;

import io.github.jangws1030.creatorsettlementapi.sale.domain.CancellationRecord;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CancellationRecordRepository extends JpaRepository<CancellationRecord, String> {

    @Query("""
            select coalesce(sum(c.refundAmount), 0)
            from CancellationRecord c
            where c.saleRecord.id = :saleId
            """)
    Long sumRefundAmountBySaleId(@Param("saleId") String saleId);

    @Query("""
            select coalesce(sum(c.refundAmount), 0)
            from CancellationRecord c
            where c.saleRecord.course.creator.id = :creatorId
              and c.cancelledAt >= :fromDateTime
              and c.cancelledAt < :toDateTime
            """)
    Long sumRefundAmountByCreatorIdAndCancelledAtBetween(
            @Param("creatorId") String creatorId,
            @Param("fromDateTime") OffsetDateTime fromDateTime,
            @Param("toDateTime") OffsetDateTime toDateTime
    );

    @Query("""
            select count(c)
            from CancellationRecord c
            where c.saleRecord.course.creator.id = :creatorId
              and c.cancelledAt >= :fromDateTime
              and c.cancelledAt < :toDateTime
            """)
    long countByCreatorIdAndCancelledAtBetween(
            @Param("creatorId") String creatorId,
            @Param("fromDateTime") OffsetDateTime fromDateTime,
            @Param("toDateTime") OffsetDateTime toDateTime
    );

    @Query("""
            select c
            from CancellationRecord c
            join fetch c.saleRecord s
            where s.course.creator.id = :creatorId
              and c.cancelledAt >= :fromDateTime
              and c.cancelledAt < :toDateTime
            """)
    List<CancellationRecord> findByCreatorIdAndCancelledAtBetweenWithSaleRecord(
            @Param("creatorId") String creatorId,
            @Param("fromDateTime") OffsetDateTime fromDateTime,
            @Param("toDateTime") OffsetDateTime toDateTime
    );
}
