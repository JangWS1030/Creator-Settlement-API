package io.github.jangws1030.creatorsettlementapi.settlement;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
class SettlementScenarioApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void tc01_appliesAccumulatedPartialRefundWithinPrincipal() throws Exception {
        createSale("sale-tc01", "course-9", 100_000L, "2025-09-10T10:00:00+09:00")
                .andExpect(status().isCreated());

        createCancellation("sale-tc01", "cancel-tc01-1", 30_000L, "2025-09-11T09:00:00+09:00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accumulatedRefundAmount").value(30000))
                .andExpect(jsonPath("$.remainingSaleAmount").value(70000));

        createCancellation("sale-tc01", "cancel-tc01-2", 20_000L, "2025-09-12T09:00:00+09:00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accumulatedRefundAmount").value(50000))
                .andExpect(jsonPath("$.remainingSaleAmount").value(50000));

        mockMvc.perform(get("/api/settlements/monthly")
                        .param("creatorId", "creator-6")
                        .param("yearMonth", "2025-09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSaleAmount").value(100000))
                .andExpect(jsonPath("$.refundAmount").value(50000))
                .andExpect(jsonPath("$.netSaleAmount").value(50000))
                .andExpect(jsonPath("$.platformFeeAmount").value(10000))
                .andExpect(jsonPath("$.scheduledSettlementAmount").value(40000))
                .andExpect(jsonPath("$.saleCount").value(1))
                .andExpect(jsonPath("$.cancellationCount").value(2));
    }

    @Test
    void tc02_keepsNegativeNetSettlementAsCarryForwardStyleResult() throws Exception {
        createSale("sale-tc02", "course-9", 50_000L, "2025-10-15T09:00:00+09:00")
                .andExpect(status().isCreated());

        createCancellation("sale-tc02", "cancel-tc02-1", 50_000L, "2025-11-03T10:00:00+09:00")
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/settlements/monthly")
                        .param("creatorId", "creator-6")
                        .param("yearMonth", "2025-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSaleAmount").value(0))
                .andExpect(jsonPath("$.refundAmount").value(50000))
                .andExpect(jsonPath("$.netSaleAmount").value(-50000))
                .andExpect(jsonPath("$.platformFeeAmount").value(-10000))
                .andExpect(jsonPath("$.scheduledSettlementAmount").value(-40000))
                .andExpect(jsonPath("$.saleCount").value(0))
                .andExpect(jsonPath("$.cancellationCount").value(1));
    }

    @Test
    void tc03_appliesPaidSettlementCancellationToNextMonthOnly() throws Exception {
        createSale("sale-tc03", "course-5", 120_000L, "2025-03-20T13:00:00+09:00")
                .andExpect(status().isCreated());

        createSettlement("creator-4", "2025-03")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.scheduledSettlementAmount").value(96000));

        confirmSettlement("settlement-creator-4-2025-03")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));

        paySettlement("settlement-creator-4-2025-03")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"));

        createCancellation("sale-tc03", "cancel-tc03-1", 50_000L, "2025-04-02T09:00:00+09:00")
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/settlements/monthly")
                        .param("creatorId", "creator-4")
                        .param("yearMonth", "2025-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settlementId").value("settlement-creator-4-2025-03"))
                .andExpect(jsonPath("$.settlementStatus").value("PAID"))
                .andExpect(jsonPath("$.totalSaleAmount").value(120000))
                .andExpect(jsonPath("$.refundAmount").value(0))
                .andExpect(jsonPath("$.scheduledSettlementAmount").value(96000));

        mockMvc.perform(get("/api/settlements/monthly")
                        .param("creatorId", "creator-4")
                        .param("yearMonth", "2025-04"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settlementStatus").value("NOT_CREATED"))
                .andExpect(jsonPath("$.totalSaleAmount").value(0))
                .andExpect(jsonPath("$.refundAmount").value(50000))
                .andExpect(jsonPath("$.netSaleAmount").value(-50000))
                .andExpect(jsonPath("$.platformFeeAmount").value(-10000))
                .andExpect(jsonPath("$.scheduledSettlementAmount").value(-40000));
    }

    @Test
    void tc04_usesSaleDateFeeRateWhenRefundHappensAfterRateChange() throws Exception {
        createFeeRate("2025-01-01", 10)
                .andExpect(status().isCreated());
        createFeeRate("2025-04-01", 30)
                .andExpect(status().isCreated());

        createSale("sale-tc04", "course-7", 9_000L, "2025-03-10T09:00:00+09:00")
                .andExpect(status().isCreated());

        createCancellation("sale-tc04", "cancel-tc04-1", 9_000L, "2025-04-05T10:00:00+09:00")
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/settlements/monthly")
                        .param("creatorId", "creator-5")
                        .param("yearMonth", "2025-04"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feeRatePercentage").value(30))
                .andExpect(jsonPath("$.totalSaleAmount").value(0))
                .andExpect(jsonPath("$.refundAmount").value(9000))
                .andExpect(jsonPath("$.netSaleAmount").value(-9000))
                .andExpect(jsonPath("$.platformFeeAmount").value(-900))
                .andExpect(jsonPath("$.scheduledSettlementAmount").value(-8100));
    }

    @Test
    void tc05_separatesMonthBoundaryAtMillisecondPrecisionInKst() throws Exception {
        createSale("sale-tc05-1", "course-5", 10_000L, "2025-08-31T23:59:59.999+09:00")
                .andExpect(status().isCreated());
        createSale("sale-tc05-2", "course-5", 20_000L, "2025-09-01T00:00:00.000+09:00")
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/settlements/monthly")
                        .param("creatorId", "creator-4")
                        .param("yearMonth", "2025-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSaleAmount").value(10000))
                .andExpect(jsonPath("$.refundAmount").value(0))
                .andExpect(jsonPath("$.netSaleAmount").value(10000))
                .andExpect(jsonPath("$.platformFeeAmount").value(2000))
                .andExpect(jsonPath("$.scheduledSettlementAmount").value(8000))
                .andExpect(jsonPath("$.saleCount").value(1));

        mockMvc.perform(get("/api/settlements/monthly")
                        .param("creatorId", "creator-4")
                        .param("yearMonth", "2025-09"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSaleAmount").value(20000))
                .andExpect(jsonPath("$.refundAmount").value(0))
                .andExpect(jsonPath("$.netSaleAmount").value(20000))
                .andExpect(jsonPath("$.platformFeeAmount").value(4000))
                .andExpect(jsonPath("$.scheduledSettlementAmount").value(16000))
                .andExpect(jsonPath("$.saleCount").value(1));
    }

    private ResultActions createSale(
            String id,
            String courseId,
            long amount,
            String paidAt
    ) throws Exception {
        return mockMvc.perform(post("/api/sales")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "id", id,
                        "courseId", courseId,
                        "studentId", "scenario-student-" + id,
                        "amount", amount,
                        "paidAt", paidAt
                ))));
    }

    private ResultActions createCancellation(
            String saleId,
            String id,
            long refundAmount,
            String cancelledAt
    ) throws Exception {
        return mockMvc.perform(post("/api/sales/" + saleId + "/cancellations")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "id", id,
                        "refundAmount", refundAmount,
                        "cancelledAt", cancelledAt
                ))));
    }

    private ResultActions createSettlement(String creatorId, String yearMonth) throws Exception {
        return mockMvc.perform(post("/api/settlements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "creatorId", creatorId,
                        "yearMonth", yearMonth
                ))));
    }

    private ResultActions confirmSettlement(String settlementId) throws Exception {
        return mockMvc.perform(patch("/api/settlements/" + settlementId + "/confirm"));
    }

    private ResultActions paySettlement(String settlementId) throws Exception {
        return mockMvc.perform(patch("/api/settlements/" + settlementId + "/pay"));
    }

    private ResultActions createFeeRate(String effectiveFrom, int feeRatePercentage) throws Exception {
        return mockMvc.perform(post("/api/admin/fee-rates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "effectiveFrom", effectiveFrom,
                        "feeRatePercentage", feeRatePercentage
                ))));
    }
}
