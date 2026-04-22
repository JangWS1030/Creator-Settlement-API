package io.github.jangws1030.creatorsettlementapi.settlement.api;

import io.github.jangws1030.creatorsettlementapi.settlement.application.FeeRateService;
import io.github.jangws1030.creatorsettlementapi.settlement.application.SettlementCommandService;
import io.github.jangws1030.creatorsettlementapi.settlement.application.SettlementCsvService;
import io.github.jangws1030.creatorsettlementapi.settlement.application.SettlementQueryService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SettlementController {

    private final SettlementQueryService settlementQueryService;
    private final SettlementCommandService settlementCommandService;
    private final SettlementCsvService settlementCsvService;
    private final FeeRateService feeRateService;

    public SettlementController(
            SettlementQueryService settlementQueryService,
            SettlementCommandService settlementCommandService,
            SettlementCsvService settlementCsvService,
            FeeRateService feeRateService
    ) {
        this.settlementQueryService = settlementQueryService;
        this.settlementCommandService = settlementCommandService;
        this.settlementCsvService = settlementCsvService;
        this.feeRateService = feeRateService;
    }

    @GetMapping("/settlements/monthly")
    public SettlementApiDtos.MonthlySettlementResponse getMonthlySettlement(
            @RequestParam String creatorId,
            @RequestParam String yearMonth
    ) {
        return settlementQueryService.getMonthlySettlement(creatorId, yearMonth);
    }

    @PostMapping("/settlements")
    public ResponseEntity<SettlementApiDtos.SettlementResponse> createSettlement(
            @Valid @RequestBody SettlementApiDtos.CreateSettlementRequest request
    ) {
        SettlementApiDtos.SettlementResponse response = settlementCommandService.createMonthlySettlement(request);
        return ResponseEntity.status(201).body(response);
    }

    @GetMapping("/settlements/{settlementId}")
    public SettlementApiDtos.SettlementResponse getSettlement(@PathVariable String settlementId) {
        return settlementQueryService.getSettlement(settlementId);
    }

    @PatchMapping("/settlements/{settlementId}/confirm")
    public SettlementApiDtos.SettlementResponse confirmSettlement(@PathVariable String settlementId) {
        return settlementCommandService.confirm(settlementId);
    }

    @PatchMapping("/settlements/{settlementId}/pay")
    public SettlementApiDtos.SettlementResponse paySettlement(@PathVariable String settlementId) {
        return settlementCommandService.markPaid(settlementId);
    }

    @GetMapping("/admin/settlements")
    public SettlementApiDtos.AdminSettlementResponse getAdminSettlement(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        return settlementQueryService.getAdminSettlement(startDate, endDate);
    }

    @GetMapping(value = "/admin/settlements.csv", produces = "text/csv; charset=UTF-8")
    public ResponseEntity<String> downloadAdminSettlementCsv(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        String csv = settlementCsvService.buildAdminSettlementCsv(startDate, endDate);
        String filename = "settlements-" + startDate + "-" + endDate + ".csv";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(MediaType.parseMediaType("text/csv; charset=UTF-8"))
                .body(csv);
    }

    @GetMapping("/admin/fee-rates")
    public List<SettlementApiDtos.FeeRateResponse> getFeeRates() {
        return feeRateService.getFeeRates();
    }

    @PostMapping("/admin/fee-rates")
    public ResponseEntity<SettlementApiDtos.FeeRateResponse> createFeeRate(
            @Valid @RequestBody SettlementApiDtos.CreateFeeRateRequest request
    ) {
        return ResponseEntity.status(201).body(feeRateService.createFeeRate(request));
    }
}
