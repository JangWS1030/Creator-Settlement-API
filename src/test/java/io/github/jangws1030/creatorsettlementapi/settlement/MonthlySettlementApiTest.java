package io.github.jangws1030.creatorsettlementapi.settlement;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
class MonthlySettlementApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void returnsMarchSettlementForCreatorOne() throws Exception {
        mockMvc.perform(get("/api/settlements/monthly")
                        .param("creatorId", "creator-1")
                        .param("yearMonth", "2025-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creatorId").value("creator-1"))
                .andExpect(jsonPath("$.creatorName").value("김강사"))
                .andExpect(jsonPath("$.yearMonth").value("2025-03"))
                .andExpect(jsonPath("$.totalSaleAmount").value(260000))
                .andExpect(jsonPath("$.refundAmount").value(110000))
                .andExpect(jsonPath("$.netSaleAmount").value(150000))
                .andExpect(jsonPath("$.platformFeeAmount").value(30000))
                .andExpect(jsonPath("$.scheduledSettlementAmount").value(120000))
                .andExpect(jsonPath("$.saleCount").value(4))
                .andExpect(jsonPath("$.cancellationCount").value(2))
                .andExpect(jsonPath("$.feeRatePercentage").value(20));
    }

    @Test
    void appliesMonthBoundaryRules() throws Exception {
        mockMvc.perform(get("/api/settlements/monthly")
                        .param("creatorId", "creator-2")
                        .param("yearMonth", "2025-01"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSaleAmount").value(60000))
                .andExpect(jsonPath("$.refundAmount").value(0))
                .andExpect(jsonPath("$.netSaleAmount").value(60000))
                .andExpect(jsonPath("$.platformFeeAmount").value(12000))
                .andExpect(jsonPath("$.scheduledSettlementAmount").value(48000));

        mockMvc.perform(get("/api/settlements/monthly")
                        .param("creatorId", "creator-2")
                        .param("yearMonth", "2025-02"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSaleAmount").value(0))
                .andExpect(jsonPath("$.refundAmount").value(60000))
                .andExpect(jsonPath("$.netSaleAmount").value(-60000))
                .andExpect(jsonPath("$.platformFeeAmount").value(-12000))
                .andExpect(jsonPath("$.scheduledSettlementAmount").value(-48000));
    }

    @Test
    void appliesMultipleCancellationsInSameMonth() throws Exception {
        mockMvc.perform(get("/api/settlements/monthly")
                        .param("creatorId", "creator-1")
                        .param("yearMonth", "2025-04"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSaleAmount").value(190000))
                .andExpect(jsonPath("$.refundAmount").value(50000))
                .andExpect(jsonPath("$.netSaleAmount").value(140000))
                .andExpect(jsonPath("$.platformFeeAmount").value(28000))
                .andExpect(jsonPath("$.scheduledSettlementAmount").value(112000))
                .andExpect(jsonPath("$.saleCount").value(2))
                .andExpect(jsonPath("$.cancellationCount").value(2));
    }

    @Test
    void appliesExactMonthBoundaryCancellationToNextMonth() throws Exception {
        mockMvc.perform(get("/api/settlements/monthly")
                        .param("creatorId", "creator-1")
                        .param("yearMonth", "2025-05"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSaleAmount").value(0))
                .andExpect(jsonPath("$.refundAmount").value(90000))
                .andExpect(jsonPath("$.netSaleAmount").value(-90000))
                .andExpect(jsonPath("$.platformFeeAmount").value(-18000))
                .andExpect(jsonPath("$.scheduledSettlementAmount").value(-72000))
                .andExpect(jsonPath("$.saleCount").value(0))
                .andExpect(jsonPath("$.cancellationCount").value(1));
    }

    @Test
    void returnsFutureDatedSettlementWhenFutureSaleDataExists() throws Exception {
        mockMvc.perform(get("/api/settlements/monthly")
                        .param("creatorId", "creator-2")
                        .param("yearMonth", "2026-12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSaleAmount").value(150000))
                .andExpect(jsonPath("$.refundAmount").value(0))
                .andExpect(jsonPath("$.netSaleAmount").value(150000))
                .andExpect(jsonPath("$.platformFeeAmount").value(30000))
                .andExpect(jsonPath("$.scheduledSettlementAmount").value(120000))
                .andExpect(jsonPath("$.saleCount").value(1))
                .andExpect(jsonPath("$.cancellationCount").value(0));
    }

    @Test
    void appliesHalfUpFeeRounding() throws Exception {
        mockMvc.perform(get("/api/settlements/monthly")
                        .param("creatorId", "creator-2")
                        .param("yearMonth", "2025-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSaleAmount").value(33333))
                .andExpect(jsonPath("$.refundAmount").value(0))
                .andExpect(jsonPath("$.netSaleAmount").value(33333))
                .andExpect(jsonPath("$.platformFeeAmount").value(6667))
                .andExpect(jsonPath("$.scheduledSettlementAmount").value(26666));
    }

    @Test
    void allowsCancellationAtSameInstantAsPaidAt() throws Exception {
        mockMvc.perform(get("/api/settlements/monthly")
                        .param("creatorId", "creator-3")
                        .param("yearMonth", "2025-04"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSaleAmount").value(40000))
                .andExpect(jsonPath("$.refundAmount").value(40000))
                .andExpect(jsonPath("$.netSaleAmount").value(0))
                .andExpect(jsonPath("$.platformFeeAmount").value(0))
                .andExpect(jsonPath("$.scheduledSettlementAmount").value(0))
                .andExpect(jsonPath("$.saleCount").value(1))
                .andExpect(jsonPath("$.cancellationCount").value(1));
    }

    @Test
    void returnsZeroForEmptyMonth() throws Exception {
        mockMvc.perform(get("/api/settlements/monthly")
                        .param("creatorId", "creator-3")
                        .param("yearMonth", "2025-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSaleAmount").value(0))
                .andExpect(jsonPath("$.refundAmount").value(0))
                .andExpect(jsonPath("$.netSaleAmount").value(0))
                .andExpect(jsonPath("$.scheduledSettlementAmount").value(0))
                .andExpect(jsonPath("$.saleCount").value(0))
                .andExpect(jsonPath("$.cancellationCount").value(0));
    }

    @Test
    void returnsJuneSettlementForCreatorFour() throws Exception {
        mockMvc.perform(get("/api/settlements/monthly")
                        .param("creatorId", "creator-4")
                        .param("yearMonth", "2025-06"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSaleAmount").value(205000))
                .andExpect(jsonPath("$.refundAmount").value(20000))
                .andExpect(jsonPath("$.netSaleAmount").value(185000))
                .andExpect(jsonPath("$.platformFeeAmount").value(37000))
                .andExpect(jsonPath("$.scheduledSettlementAmount").value(148000))
                .andExpect(jsonPath("$.saleCount").value(2))
                .andExpect(jsonPath("$.cancellationCount").value(1));
    }

    @Test
    void appliesMonthBoundaryCancellationForCreatorFive() throws Exception {
        mockMvc.perform(get("/api/settlements/monthly")
                        .param("creatorId", "creator-5")
                        .param("yearMonth", "2025-07"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSaleAmount").value(163000))
                .andExpect(jsonPath("$.refundAmount").value(0))
                .andExpect(jsonPath("$.netSaleAmount").value(163000))
                .andExpect(jsonPath("$.platformFeeAmount").value(32600))
                .andExpect(jsonPath("$.scheduledSettlementAmount").value(130400))
                .andExpect(jsonPath("$.saleCount").value(2))
                .andExpect(jsonPath("$.cancellationCount").value(0));

        mockMvc.perform(get("/api/settlements/monthly")
                        .param("creatorId", "creator-5")
                        .param("yearMonth", "2025-08"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSaleAmount").value(0))
                .andExpect(jsonPath("$.refundAmount").value(88000))
                .andExpect(jsonPath("$.netSaleAmount").value(-88000))
                .andExpect(jsonPath("$.platformFeeAmount").value(-17600))
                .andExpect(jsonPath("$.scheduledSettlementAmount").value(-70400))
                .andExpect(jsonPath("$.saleCount").value(0))
                .andExpect(jsonPath("$.cancellationCount").value(1));
    }

    @Test
    void rejectsInvalidYearMonthFormat() throws Exception {
        mockMvc.perform(get("/api/settlements/monthly")
                        .param("creatorId", "creator-1")
                        .param("yearMonth", "2025/03"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("yearMonth must follow yyyy-MM format."));
    }

    @Test
    void rejectsMonthlySettlementWhenYearMonthIsMissing() throws Exception {
        mockMvc.perform(get("/api/settlements/monthly")
                        .param("creatorId", "creator-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing request parameter: yearMonth"));
    }

    @Test
    void rejectsMonthlySettlementWhenCreatorDoesNotExist() throws Exception {
        mockMvc.perform(get("/api/settlements/monthly")
                        .param("creatorId", "creator-missing")
                        .param("yearMonth", "2025-03"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Creator not found: creator-missing"));
    }
}
