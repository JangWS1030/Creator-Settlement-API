package io.github.jangws1030.creatorsettlementapi.sale.api;

import io.github.jangws1030.creatorsettlementapi.sale.application.SaleCommandService;
import io.github.jangws1030.creatorsettlementapi.sale.application.SaleQueryService;
import jakarta.validation.Valid;
import java.net.URI;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SaleController {

    private final SaleCommandService saleCommandService;
    private final SaleQueryService saleQueryService;

    public SaleController(
            SaleCommandService saleCommandService,
            SaleQueryService saleQueryService
    ) {
        this.saleCommandService = saleCommandService;
        this.saleQueryService = saleQueryService;
    }

    @PostMapping("/sales")
    public ResponseEntity<SaleApiDtos.CreateSaleResponse> createSale(
            @Valid @RequestBody SaleApiDtos.CreateSaleRequest request
    ) {
        SaleApiDtos.CreateSaleResponse response = saleCommandService.createSale(request);
        return ResponseEntity.created(URI.create("/api/sales/" + response.id())).body(response);
    }

    @PostMapping("/sales/{saleId}/cancellations")
    public ResponseEntity<SaleApiDtos.CreateCancellationResponse> createCancellation(
            @PathVariable String saleId,
            @Valid @RequestBody SaleApiDtos.CreateCancellationRequest request
    ) {
        SaleApiDtos.CreateCancellationResponse response = saleCommandService.createCancellation(saleId, request);
        return ResponseEntity.created(URI.create("/api/sales/" + saleId + "/cancellations/" + response.id()))
                .body(response);
    }

    @GetMapping("/sales")
    public SaleApiDtos.SaleListResponse getSales(
            @RequestParam String creatorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate
    ) {
        return saleQueryService.getSales(creatorId, fromDate, toDate);
    }
}
