package io.github.jangws1030.creatorsettlementapi.sale.application;

import io.github.jangws1030.creatorsettlementapi.common.exception.BadRequestException;
import io.github.jangws1030.creatorsettlementapi.common.exception.NotFoundException;
import io.github.jangws1030.creatorsettlementapi.creator.domain.Creator;
import io.github.jangws1030.creatorsettlementapi.creator.repository.CreatorRepository;
import io.github.jangws1030.creatorsettlementapi.sale.api.SaleApiDtos;
import io.github.jangws1030.creatorsettlementapi.sale.domain.SaleRecord;
import io.github.jangws1030.creatorsettlementapi.sale.repository.SaleRecordRepository;
import io.github.jangws1030.creatorsettlementapi.support.KstTimeWindowFactory;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SaleQueryService {

    private final CreatorRepository creatorRepository;
    private final SaleRecordRepository saleRecordRepository;

    public SaleQueryService(
            CreatorRepository creatorRepository,
            SaleRecordRepository saleRecordRepository
    ) {
        this.creatorRepository = creatorRepository;
        this.saleRecordRepository = saleRecordRepository;
    }

    @Transactional(readOnly = true)
    public SaleApiDtos.SaleListResponse getSales(
            String creatorId,
            LocalDate fromDate,
            LocalDate toDate
    ) {
        Creator creator = creatorRepository.findById(creatorId)
                .orElseThrow(() -> new NotFoundException("Creator not found: " + creatorId));

        validateDateRange(fromDate, toDate);
        OffsetDateTime fromDateTime = fromDate == null ? null : KstTimeWindowFactory.atStartOfDay(fromDate);
        OffsetDateTime toDateTime = toDate == null ? null : KstTimeWindowFactory.atStartOfDay(toDate.plusDays(1));

        List<SaleRecord> saleRecords = saleRecordRepository.findByCreatorIdWithDetails(
                creatorId,
                fromDateTime,
                toDateTime
        );

        List<SaleApiDtos.SaleItemResponse> items = saleRecords.stream()
                .map(this::toSaleItemResponse)
                .toList();

        long totalSaleAmount = saleRecords.stream()
                .mapToLong(SaleRecord::getAmount)
                .sum();
        long totalRefundAmount = saleRecords.stream()
                .mapToLong(SaleRecord::getRefundedAmount)
                .sum();

        return new SaleApiDtos.SaleListResponse(
                creator.getId(),
                creator.getName(),
                fromDate,
                toDate,
                totalSaleAmount,
                totalRefundAmount,
                items.size(),
                items
        );
    }

    private SaleApiDtos.SaleItemResponse toSaleItemResponse(SaleRecord saleRecord) {
        long refundedAmount = saleRecord.getRefundedAmount();
        long remainingAmount = saleRecord.getAmount() - refundedAmount;
        String saleStatus = resolveSaleStatus(saleRecord.getAmount(), refundedAmount);

        List<SaleApiDtos.CancellationItemResponse> cancellations = saleRecord.getCancellations()
                .stream()
                .map(cancellationRecord -> new SaleApiDtos.CancellationItemResponse(
                        cancellationRecord.getId(),
                        cancellationRecord.getRefundAmount(),
                        cancellationRecord.getCancelledAt()
                ))
                .toList();

        return new SaleApiDtos.SaleItemResponse(
                saleRecord.getId(),
                saleRecord.getCourse().getId(),
                saleRecord.getCourse().getTitle(),
                saleRecord.getStudentId(),
                saleRecord.getAmount(),
                saleRecord.getPaidAt(),
                refundedAmount,
                remainingAmount,
                saleStatus,
                cancellations
        );
    }

    private String resolveSaleStatus(long amount, long refundedAmount) {
        if (refundedAmount == 0L) {
            return "COMPLETED";
        }
        if (refundedAmount == amount) {
            return "FULLY_REFUNDED";
        }
        return "PARTIALLY_REFUNDED";
    }

    private void validateDateRange(LocalDate fromDate, LocalDate toDate) {
        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new BadRequestException("fromDate must be before or equal to toDate.");
        }
    }
}
