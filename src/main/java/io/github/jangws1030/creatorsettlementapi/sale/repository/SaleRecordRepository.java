package io.github.jangws1030.creatorsettlementapi.sale.repository;

import io.github.jangws1030.creatorsettlementapi.sale.domain.SaleRecord;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SaleRecordRepository extends JpaRepository<SaleRecord, String> {

    @Query("""
            select distinct s
            from SaleRecord s
            join fetch s.course c
            join fetch c.creator
            left join fetch s.cancellations
            where s.id = :saleId
            """)
    Optional<SaleRecord> findWithDetailsById(@Param("saleId") String saleId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            select distinct s
            from SaleRecord s
            join fetch s.course c
            join fetch c.creator
            left join fetch s.cancellations
            where s.id = :saleId
            """)
    Optional<SaleRecord> findWithDetailsByIdForUpdate(@Param("saleId") String saleId);

    @Query("""
            select distinct s
            from SaleRecord s
            join fetch s.course c
            join fetch c.creator
            left join fetch s.cancellations
            where c.creator.id = :creatorId
              and (:fromDateTime is null or s.paidAt >= :fromDateTime)
              and (:toDateTime is null or s.paidAt < :toDateTime)
            order by s.paidAt desc, s.id desc
            """)
    List<SaleRecord> findByCreatorIdWithDetails(
            @Param("creatorId") String creatorId,
            @Param("fromDateTime") OffsetDateTime fromDateTime,
            @Param("toDateTime") OffsetDateTime toDateTime
    );

    @Query("""
            select coalesce(sum(s.amount), 0)
            from SaleRecord s
            where s.course.creator.id = :creatorId
              and s.paidAt >= :fromDateTime
              and s.paidAt < :toDateTime
            """)
    Long sumAmountByCreatorIdAndPaidAtBetween(
            @Param("creatorId") String creatorId,
            @Param("fromDateTime") OffsetDateTime fromDateTime,
            @Param("toDateTime") OffsetDateTime toDateTime
    );

    @Query("""
            select count(s)
            from SaleRecord s
            where s.course.creator.id = :creatorId
              and s.paidAt >= :fromDateTime
              and s.paidAt < :toDateTime
            """)
    long countByCreatorIdAndPaidAtBetween(
            @Param("creatorId") String creatorId,
            @Param("fromDateTime") OffsetDateTime fromDateTime,
            @Param("toDateTime") OffsetDateTime toDateTime
    );

    @Query("""
            select s
            from SaleRecord s
            where s.course.creator.id = :creatorId
              and s.paidAt >= :fromDateTime
              and s.paidAt < :toDateTime
            """)
    List<SaleRecord> findByCreatorIdAndPaidAtBetweenForSettlement(
            @Param("creatorId") String creatorId,
            @Param("fromDateTime") OffsetDateTime fromDateTime,
            @Param("toDateTime") OffsetDateTime toDateTime
    );
}
