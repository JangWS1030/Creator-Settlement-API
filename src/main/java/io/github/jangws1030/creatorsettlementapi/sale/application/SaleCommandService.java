package io.github.jangws1030.creatorsettlementapi.sale.application;

import io.github.jangws1030.creatorsettlementapi.common.exception.BadRequestException;
import io.github.jangws1030.creatorsettlementapi.common.exception.ConflictException;
import io.github.jangws1030.creatorsettlementapi.common.exception.NotFoundException;
import io.github.jangws1030.creatorsettlementapi.course.domain.Course;
import io.github.jangws1030.creatorsettlementapi.course.repository.CourseRepository;
import io.github.jangws1030.creatorsettlementapi.sale.api.SaleApiDtos;
import io.github.jangws1030.creatorsettlementapi.sale.domain.CancellationRecord;
import io.github.jangws1030.creatorsettlementapi.sale.domain.SaleRecord;
import io.github.jangws1030.creatorsettlementapi.sale.repository.CancellationRecordRepository;
import io.github.jangws1030.creatorsettlementapi.sale.repository.SaleRecordRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class SaleCommandService {

    private final CourseRepository courseRepository;
    private final SaleRecordRepository saleRecordRepository;
    private final CancellationRecordRepository cancellationRecordRepository;

    public SaleCommandService(
            CourseRepository courseRepository,
            SaleRecordRepository saleRecordRepository,
            CancellationRecordRepository cancellationRecordRepository
    ) {
        this.courseRepository = courseRepository;
        this.saleRecordRepository = saleRecordRepository;
        this.cancellationRecordRepository = cancellationRecordRepository;
    }

    @Transactional
    public SaleApiDtos.CreateSaleResponse createSale(SaleApiDtos.CreateSaleRequest request) {
        Course course = courseRepository.findById(request.courseId())
                .orElseThrow(() -> new NotFoundException("Course not found: " + request.courseId()));

        String saleId = resolveId("sale", request.id());
        if (saleRecordRepository.existsById(saleId)) {
            throw new ConflictException("Sale already exists: " + saleId);
        }

        SaleRecord saleRecord = new SaleRecord(
                saleId,
                course,
                request.studentId().trim(),
                request.amount(),
                request.paidAt()
        );
        saleRecordRepository.save(saleRecord);

        return new SaleApiDtos.CreateSaleResponse(
                saleRecord.getId(),
                course.getCreator().getId(),
                course.getCreator().getName(),
                course.getId(),
                course.getTitle(),
                saleRecord.getStudentId(),
                saleRecord.getAmount(),
                saleRecord.getPaidAt()
        );
    }

    @Transactional
    public SaleApiDtos.CreateCancellationResponse createCancellation(
            String saleId,
            SaleApiDtos.CreateCancellationRequest request
    ) {
        SaleRecord saleRecord = saleRecordRepository.findWithDetailsByIdForUpdate(saleId)
                .orElseThrow(() -> new NotFoundException("Sale not found: " + saleId));

        String cancellationId = resolveId("cancel", request.id());
        if (cancellationRecordRepository.existsById(cancellationId)) {
            throw new ConflictException("Cancellation already exists: " + cancellationId);
        }

        if (request.cancelledAt().isBefore(saleRecord.getPaidAt())) {
            throw new BadRequestException("cancelledAt must be after or equal to paidAt.");
        }

        long accumulatedRefundAmount = defaultLong(
                cancellationRecordRepository.sumRefundAmountBySaleId(saleId)
        );
        long nextRefundAmount = accumulatedRefundAmount + request.refundAmount();
        if (nextRefundAmount > saleRecord.getAmount()) {
            throw new BadRequestException("Refund amount exceeds original sale amount.");
        }

        CancellationRecord cancellationRecord = new CancellationRecord(
                cancellationId,
                saleRecord,
                request.refundAmount(),
                request.cancelledAt()
        );
        saleRecord.addCancellation(cancellationRecord);
        saleRecordRepository.save(saleRecord);

        return new SaleApiDtos.CreateCancellationResponse(
                cancellationRecord.getId(),
                saleRecord.getId(),
                cancellationRecord.getRefundAmount(),
                cancellationRecord.getCancelledAt(),
                nextRefundAmount,
                saleRecord.getAmount() - nextRefundAmount
        );
    }

    private String resolveId(String prefix, String rawId) {
        if (StringUtils.hasText(rawId)) {
            return rawId.trim();
        }
        return prefix + "-" + UUID.randomUUID();
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }
}
