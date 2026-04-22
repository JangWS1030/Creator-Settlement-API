package io.github.jangws1030.creatorsettlementapi.settlement.application;

import io.github.jangws1030.creatorsettlementapi.common.exception.ConflictException;
import io.github.jangws1030.creatorsettlementapi.settlement.api.SettlementApiDtos;
import io.github.jangws1030.creatorsettlementapi.settlement.domain.FeeRateHistory;
import io.github.jangws1030.creatorsettlementapi.settlement.repository.FeeRateHistoryRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FeeRateService {

    public static final int DEFAULT_FEE_RATE_PERCENTAGE = 20;
    private static final ZoneOffset KST_OFFSET = ZoneOffset.ofHours(9);

    private final FeeRateHistoryRepository feeRateHistoryRepository;

    public FeeRateService(FeeRateHistoryRepository feeRateHistoryRepository) {
        this.feeRateHistoryRepository = feeRateHistoryRepository;
    }

    @Transactional(readOnly = true)
    public int getFeeRatePercentage(LocalDate targetDate) {
        return feeRateHistoryRepository.findTopByEffectiveFromLessThanEqualOrderByEffectiveFromDesc(targetDate)
                .map(FeeRateHistory::getFeeRatePercentage)
                .orElse(DEFAULT_FEE_RATE_PERCENTAGE);
    }

    @Transactional(readOnly = true)
    public List<SettlementApiDtos.FeeRateResponse> getFeeRates() {
        return feeRateHistoryRepository.findAllByOrderByEffectiveFromAsc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public SettlementApiDtos.FeeRateResponse createFeeRate(SettlementApiDtos.CreateFeeRateRequest request) {
        if (feeRateHistoryRepository.existsByEffectiveFrom(request.effectiveFrom())) {
            throw new ConflictException("Fee rate already exists for effectiveFrom: " + request.effectiveFrom());
        }

        FeeRateHistory feeRateHistory = new FeeRateHistory(
                request.effectiveFrom(),
                request.feeRatePercentage(),
                OffsetDateTime.now(KST_OFFSET)
        );
        try {
            return toResponse(feeRateHistoryRepository.saveAndFlush(feeRateHistory));
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("Fee rate already exists for effectiveFrom: " + request.effectiveFrom());
        }
    }

    private SettlementApiDtos.FeeRateResponse toResponse(FeeRateHistory feeRateHistory) {
        return new SettlementApiDtos.FeeRateResponse(
                feeRateHistory.getId(),
                feeRateHistory.getEffectiveFrom(),
                feeRateHistory.getFeeRatePercentage(),
                feeRateHistory.getCreatedAt()
        );
    }
}
