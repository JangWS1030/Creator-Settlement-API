package io.github.jangws1030.creatorsettlementapi.settlement.application;

import io.github.jangws1030.creatorsettlementapi.settlement.api.SettlementApiDtos;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettlementCsvService {

    private static final String HEADER = String.join(",",
            "creatorId",
            "creatorName",
            "totalSaleAmount",
            "refundAmount",
            "netSaleAmount",
            "platformFeeAmount",
            "scheduledSettlementAmount",
            "saleCount",
            "cancellationCount",
            "feeRatePercentage"
    );

    private final SettlementQueryService settlementQueryService;

    public SettlementCsvService(SettlementQueryService settlementQueryService) {
        this.settlementQueryService = settlementQueryService;
    }

    @Transactional(readOnly = true)
    public String buildAdminSettlementCsv(LocalDate startDate, LocalDate endDate) {
        SettlementApiDtos.AdminSettlementResponse response = settlementQueryService.getAdminSettlement(startDate, endDate);
        StringBuilder csv = new StringBuilder();
        csv.append('\ufeff');
        csv.append(HEADER).append('\n');

        for (SettlementApiDtos.AdminCreatorSettlementResponse creatorSettlement : response.creatorSettlements()) {
            appendRow(
                    csv,
                    creatorSettlement.creatorId(),
                    creatorSettlement.creatorName(),
                    creatorSettlement.totalSaleAmount(),
                    creatorSettlement.refundAmount(),
                    creatorSettlement.netSaleAmount(),
                    creatorSettlement.platformFeeAmount(),
                    creatorSettlement.scheduledSettlementAmount(),
                    creatorSettlement.saleCount(),
                    creatorSettlement.cancellationCount(),
                    creatorSettlement.feeRatePercentage()
            );
        }

        appendRow(
                csv,
                "TOTAL",
                "",
                response.totalSaleAmount(),
                response.totalRefundAmount(),
                response.totalNetSaleAmount(),
                response.totalPlatformFeeAmount(),
                response.totalScheduledSettlementAmount(),
                0,
                0,
                0
        );
        return csv.toString();
    }

    private void appendRow(
            StringBuilder csv,
            String creatorId,
            String creatorName,
            long totalSaleAmount,
            long refundAmount,
            long netSaleAmount,
            long platformFeeAmount,
            long scheduledSettlementAmount,
            long saleCount,
            long cancellationCount,
            int feeRatePercentage
    ) {
        csv.append(escape(creatorId)).append(',')
                .append(escape(creatorName)).append(',')
                .append(totalSaleAmount).append(',')
                .append(refundAmount).append(',')
                .append(netSaleAmount).append(',')
                .append(platformFeeAmount).append(',')
                .append(scheduledSettlementAmount).append(',')
                .append(saleCount).append(',')
                .append(cancellationCount).append(',')
                .append(feeRatePercentage)
                .append('\n');
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return '"' + value.replace("\"", "\"\"") + '"';
    }
}
