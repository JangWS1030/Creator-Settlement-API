package io.github.jangws1030.creatorsettlementapi.settlement.application;

import io.github.jangws1030.creatorsettlementapi.common.exception.NotFoundException;
import io.github.jangws1030.creatorsettlementapi.creator.domain.Creator;
import io.github.jangws1030.creatorsettlementapi.creator.repository.CreatorRepository;
import io.github.jangws1030.creatorsettlementapi.sale.domain.CancellationRecord;
import io.github.jangws1030.creatorsettlementapi.sale.domain.SaleRecord;
import io.github.jangws1030.creatorsettlementapi.sale.repository.CancellationRecordRepository;
import io.github.jangws1030.creatorsettlementapi.sale.repository.SaleRecordRepository;
import io.github.jangws1030.creatorsettlementapi.settlement.api.SettlementApiDtos;
import io.github.jangws1030.creatorsettlementapi.settlement.domain.Settlement;
import io.github.jangws1030.creatorsettlementapi.settlement.repository.SettlementRepository;
import io.github.jangws1030.creatorsettlementapi.support.KstTimeWindowFactory;
import io.github.jangws1030.creatorsettlementapi.support.TimeWindow;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.YearMonth;
import java.util.List;
import java.time.ZoneOffset;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettlementQueryService {

    private static final ZoneOffset KST_OFFSET = ZoneOffset.ofHours(9);

    private final CreatorRepository creatorRepository;
    private final SaleRecordRepository saleRecordRepository;
    private final CancellationRecordRepository cancellationRecordRepository;
    private final SettlementRepository settlementRepository;
    private final SettlementCalculator settlementCalculator;
    private final FeeRateService feeRateService;
    private final SettlementResponseMapper settlementResponseMapper;

    public SettlementQueryService(
            CreatorRepository creatorRepository,
            SaleRecordRepository saleRecordRepository,
            CancellationRecordRepository cancellationRecordRepository,
            SettlementRepository settlementRepository,
            SettlementCalculator settlementCalculator,
            FeeRateService feeRateService,
            SettlementResponseMapper settlementResponseMapper
    ) {
        this.creatorRepository = creatorRepository;
        this.saleRecordRepository = saleRecordRepository;
        this.cancellationRecordRepository = cancellationRecordRepository;
        this.settlementRepository = settlementRepository;
        this.settlementCalculator = settlementCalculator;
        this.feeRateService = feeRateService;
        this.settlementResponseMapper = settlementResponseMapper;
    }

    @Transactional(readOnly = true)
    public SettlementApiDtos.MonthlySettlementResponse getMonthlySettlement(
            String creatorId,
            String yearMonthText
    ) {
        YearMonth yearMonth = KstTimeWindowFactory.parseYearMonth(yearMonthText);
        return settlementRepository.findByCreatorIdAndYearMonth(creatorId, yearMonth.toString())
                .map(this::toMonthlySettlementResponse)
                .orElseGet(() -> {
                    SettlementDraft settlementDraft = calculateMonthlyDraft(creatorId, yearMonthText);
                    SettlementCalculationResult calculationResult = settlementDraft.calculationResult();

                    return new SettlementApiDtos.MonthlySettlementResponse(
                            settlementDraft.creator().getId(),
                            settlementDraft.creator().getName(),
                            settlementDraft.yearMonth().toString(),
                            calculationResult.totalSaleAmount(),
                            calculationResult.refundAmount(),
                            calculationResult.netSaleAmount(),
                            calculationResult.platformFeeAmount(),
                            calculationResult.scheduledSettlementAmount(),
                            calculationResult.saleCount(),
                            calculationResult.cancellationCount(),
                            calculationResult.feeRatePercentage(),
                            null,
                            "NOT_CREATED"
                    );
                });
    }

    @Transactional(readOnly = true)
    public SettlementApiDtos.SettlementResponse getSettlement(String settlementId) {
        Settlement settlement = settlementRepository.findById(settlementId)
                .orElseThrow(() -> new NotFoundException("Settlement not found: " + settlementId));
        return settlementResponseMapper.toResponse(settlement);
    }

    @Transactional(readOnly = true)
    public SettlementDraft calculateMonthlyDraft(String creatorId, String yearMonthText) {
        Creator creator = findCreator(creatorId);
        YearMonth yearMonth = KstTimeWindowFactory.parseYearMonth(yearMonthText);
        TimeWindow timeWindow = KstTimeWindowFactory.month(yearMonthText);
        int feeRatePercentage = feeRateService.getFeeRatePercentage(yearMonth.atDay(1));
        SettlementCalculationResult calculationResult = calculateCreatorSettlement(
                creatorId,
                timeWindow,
                feeRatePercentage
        );

        return new SettlementDraft(creator, yearMonth, calculationResult);
    }

    private SettlementApiDtos.MonthlySettlementResponse toMonthlySettlementResponse(Settlement settlement) {
        return new SettlementApiDtos.MonthlySettlementResponse(
                settlement.getCreator().getId(),
                settlement.getCreator().getName(),
                settlement.getYearMonth(),
                settlement.getTotalSaleAmount(),
                settlement.getRefundAmount(),
                settlement.getNetSaleAmount(),
                settlement.getPlatformFeeAmount(),
                settlement.getScheduledSettlementAmount(),
                settlement.getSaleCount(),
                settlement.getCancellationCount(),
                settlement.getFeeRatePercentage(),
                settlement.getId(),
                settlement.getStatus().name()
        );
    }

    @Transactional(readOnly = true)
    public SettlementApiDtos.AdminSettlementResponse getAdminSettlement(
            LocalDate startDate,
            LocalDate endDate
    ) {
        TimeWindow timeWindow = KstTimeWindowFactory.dateRange(startDate, endDate);
        int feeRatePercentage = feeRateService.getFeeRatePercentage(startDate);
        List<SettlementApiDtos.AdminCreatorSettlementResponse> creatorSettlements = creatorRepository.findAllByOrderByIdAsc()
                .stream()
                .map(creator -> toAdminCreatorSettlementResponse(creator, timeWindow, feeRatePercentage))
                .toList();

        long totalSaleAmount = creatorSettlements.stream()
                .mapToLong(SettlementApiDtos.AdminCreatorSettlementResponse::totalSaleAmount)
                .sum();
        long totalRefundAmount = creatorSettlements.stream()
                .mapToLong(SettlementApiDtos.AdminCreatorSettlementResponse::refundAmount)
                .sum();
        long totalNetSaleAmount = creatorSettlements.stream()
                .mapToLong(SettlementApiDtos.AdminCreatorSettlementResponse::netSaleAmount)
                .sum();
        long totalPlatformFeeAmount = creatorSettlements.stream()
                .mapToLong(SettlementApiDtos.AdminCreatorSettlementResponse::platformFeeAmount)
                .sum();
        long totalScheduledSettlementAmount = creatorSettlements.stream()
                .mapToLong(SettlementApiDtos.AdminCreatorSettlementResponse::scheduledSettlementAmount)
                .sum();

        return new SettlementApiDtos.AdminSettlementResponse(
                startDate,
                endDate,
                totalSaleAmount,
                totalRefundAmount,
                totalNetSaleAmount,
                totalPlatformFeeAmount,
                totalScheduledSettlementAmount,
                creatorSettlements
        );
    }

    private SettlementApiDtos.AdminCreatorSettlementResponse toAdminCreatorSettlementResponse(
            Creator creator,
            TimeWindow timeWindow,
            int feeRatePercentage
    ) {
        SettlementCalculationResult calculationResult = calculateCreatorSettlement(
                creator.getId(),
                timeWindow,
                feeRatePercentage
        );
        return new SettlementApiDtos.AdminCreatorSettlementResponse(
                creator.getId(),
                creator.getName(),
                calculationResult.totalSaleAmount(),
                calculationResult.refundAmount(),
                calculationResult.netSaleAmount(),
                calculationResult.platformFeeAmount(),
                calculationResult.scheduledSettlementAmount(),
                calculationResult.saleCount(),
                calculationResult.cancellationCount(),
                calculationResult.feeRatePercentage()
        );
    }

    private SettlementCalculationResult calculateCreatorSettlement(
            String creatorId,
            TimeWindow timeWindow,
            int feeRatePercentage
    ) {
        List<SaleRecord> saleRecords = saleRecordRepository.findByCreatorIdAndPaidAtBetweenForSettlement(
                creatorId,
                timeWindow.start(),
                timeWindow.endExclusive()
        );
        List<CancellationRecord> cancellationRecords = cancellationRecordRepository.findByCreatorIdAndCancelledAtBetweenWithSaleRecord(
                creatorId,
                timeWindow.start(),
                timeWindow.endExclusive()
        );

        long totalSaleAmount = defaultLong(saleRecordRepository.sumAmountByCreatorIdAndPaidAtBetween(
                creatorId,
                timeWindow.start(),
                timeWindow.endExclusive()
        ));
        long refundAmount = defaultLong(cancellationRecordRepository.sumRefundAmountByCreatorIdAndCancelledAtBetween(
                creatorId,
                timeWindow.start(),
                timeWindow.endExclusive()
        ));
        long saleCount = saleRecordRepository.countByCreatorIdAndPaidAtBetween(
                creatorId,
                timeWindow.start(),
                timeWindow.endExclusive()
        );
        long cancellationCount = cancellationRecordRepository.countByCreatorIdAndCancelledAtBetween(
                creatorId,
                timeWindow.start(),
                timeWindow.endExclusive()
        );
        long salePlatformFeeAmount = saleRecords.stream()
                .mapToLong(this::calculateSalePlatformFeeAmount)
                .sum();
        long refundPlatformFeeAmount = cancellationRecords.stream()
                .mapToLong(this::calculateRefundPlatformFeeAmount)
                .sum();
        long netSaleAmount = totalSaleAmount - refundAmount;
        long platformFeeAmount = salePlatformFeeAmount - refundPlatformFeeAmount;
        long scheduledSettlementAmount = netSaleAmount - platformFeeAmount;

        return new SettlementCalculationResult(
                totalSaleAmount,
                refundAmount,
                netSaleAmount,
                platformFeeAmount,
                scheduledSettlementAmount,
                saleCount,
                cancellationCount,
                feeRatePercentage
        );
    }

    private long calculateSalePlatformFeeAmount(SaleRecord saleRecord) {
        int appliedRate = feeRateService.getFeeRatePercentage(toKstDate(saleRecord.getPaidAt()));
        return settlementCalculator.calculatePlatformFeeAmount(saleRecord.getAmount(), appliedRate);
    }

    private long calculateRefundPlatformFeeAmount(CancellationRecord cancellationRecord) {
        int appliedRate = feeRateService.getFeeRatePercentage(toKstDate(cancellationRecord.getSaleRecord().getPaidAt()));
        return settlementCalculator.calculatePlatformFeeAmount(cancellationRecord.getRefundAmount(), appliedRate);
    }

    private LocalDate toKstDate(OffsetDateTime dateTime) {
        return dateTime.withOffsetSameInstant(KST_OFFSET).toLocalDate();
    }

    private Creator findCreator(String creatorId) {
        return creatorRepository.findById(creatorId)
                .orElseThrow(() -> new NotFoundException("Creator not found: " + creatorId));
    }

    private long defaultLong(Long value) {
        return value == null ? 0L : value;
    }
}
