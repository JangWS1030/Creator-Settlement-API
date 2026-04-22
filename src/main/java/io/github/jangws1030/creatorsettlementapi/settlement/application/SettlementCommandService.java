package io.github.jangws1030.creatorsettlementapi.settlement.application;

import io.github.jangws1030.creatorsettlementapi.common.exception.BadRequestException;
import io.github.jangws1030.creatorsettlementapi.common.exception.ConflictException;
import io.github.jangws1030.creatorsettlementapi.common.exception.NotFoundException;
import io.github.jangws1030.creatorsettlementapi.settlement.api.SettlementApiDtos;
import io.github.jangws1030.creatorsettlementapi.settlement.domain.Settlement;
import io.github.jangws1030.creatorsettlementapi.settlement.domain.SettlementStatus;
import io.github.jangws1030.creatorsettlementapi.settlement.repository.SettlementRepository;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettlementCommandService {

    private static final ZoneOffset KST_OFFSET = ZoneOffset.ofHours(9);

    private final SettlementRepository settlementRepository;
    private final SettlementQueryService settlementQueryService;
    private final SettlementResponseMapper settlementResponseMapper;

    public SettlementCommandService(
            SettlementRepository settlementRepository,
            SettlementQueryService settlementQueryService,
            SettlementResponseMapper settlementResponseMapper
    ) {
        this.settlementRepository = settlementRepository;
        this.settlementQueryService = settlementQueryService;
        this.settlementResponseMapper = settlementResponseMapper;
    }

    @Transactional
    public SettlementApiDtos.SettlementResponse createMonthlySettlement(
            SettlementApiDtos.CreateSettlementRequest request
    ) {
        SettlementDraft settlementDraft = settlementQueryService.calculateMonthlyDraft(
                request.creatorId(),
                request.yearMonth()
        );
        String yearMonth = settlementDraft.yearMonth().toString();
        if (settlementRepository.existsByCreatorIdAndYearMonth(request.creatorId(), yearMonth)) {
            throw new ConflictException("Settlement already exists: " + request.creatorId() + " " + yearMonth);
        }

        SettlementCalculationResult calculationResult = settlementDraft.calculationResult();
        Settlement settlement = new Settlement(
                settlementDraft.creator(),
                settlementDraft.yearMonth(),
                calculationResult.totalSaleAmount(),
                calculationResult.refundAmount(),
                calculationResult.netSaleAmount(),
                calculationResult.platformFeeAmount(),
                calculationResult.scheduledSettlementAmount(),
                calculationResult.saleCount(),
                calculationResult.cancellationCount(),
                calculationResult.feeRatePercentage(),
                OffsetDateTime.now(KST_OFFSET)
        );

        try {
            return settlementResponseMapper.toResponse(settlementRepository.saveAndFlush(settlement));
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Settlement already exists: " + request.creatorId() + " " + yearMonth);
        }
    }

    @Transactional
    public SettlementApiDtos.SettlementResponse confirm(String settlementId) {
        Settlement settlement = findSettlement(settlementId);
        if (settlement.getStatus() != SettlementStatus.PENDING) {
            throw new BadRequestException("Only PENDING settlement can be confirmed.");
        }

        settlement.confirm(OffsetDateTime.now(KST_OFFSET));
        return settlementResponseMapper.toResponse(settlement);
    }

    @Transactional
    public SettlementApiDtos.SettlementResponse markPaid(String settlementId) {
        Settlement settlement = findSettlement(settlementId);
        if (settlement.getStatus() != SettlementStatus.CONFIRMED) {
            throw new BadRequestException("Only CONFIRMED settlement can be paid.");
        }

        settlement.markPaid(OffsetDateTime.now(KST_OFFSET));
        return settlementResponseMapper.toResponse(settlement);
    }

    private Settlement findSettlement(String settlementId) {
        return settlementRepository.findById(settlementId)
                .orElseThrow(() -> new NotFoundException("Settlement not found: " + settlementId));
    }
}
