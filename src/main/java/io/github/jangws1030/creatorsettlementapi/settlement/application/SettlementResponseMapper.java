package io.github.jangws1030.creatorsettlementapi.settlement.application;

import io.github.jangws1030.creatorsettlementapi.settlement.api.SettlementApiDtos;
import io.github.jangws1030.creatorsettlementapi.settlement.domain.Settlement;
import org.springframework.stereotype.Component;

@Component
public class SettlementResponseMapper {

    public SettlementApiDtos.SettlementResponse toResponse(Settlement settlement) {
        return new SettlementApiDtos.SettlementResponse(
                settlement.getId(),
                settlement.getCreator().getId(),
                settlement.getCreator().getName(),
                settlement.getYearMonth(),
                settlement.getPeriodStartDate(),
                settlement.getPeriodEndDate(),
                settlement.getTotalSaleAmount(),
                settlement.getRefundAmount(),
                settlement.getNetSaleAmount(),
                settlement.getPlatformFeeAmount(),
                settlement.getScheduledSettlementAmount(),
                settlement.getSaleCount(),
                settlement.getCancellationCount(),
                settlement.getFeeRatePercentage(),
                settlement.getStatus().name(),
                settlement.getCreatedAt(),
                settlement.getConfirmedAt(),
                settlement.getPaidAt()
        );
    }
}
