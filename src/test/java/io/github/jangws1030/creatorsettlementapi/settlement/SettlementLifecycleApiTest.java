package io.github.jangws1030.creatorsettlementapi.settlement;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
class SettlementLifecycleApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void createsPendingSettlementAndRejectsDuplicatePeriod() throws Exception {
        createMarchSettlement()
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("settlement-creator-1-2025-03"))
                .andExpect(jsonPath("$.status").value("PENDING"))
                .andExpect(jsonPath("$.totalSaleAmount").value(260000))
                .andExpect(jsonPath("$.refundAmount").value(110000))
                .andExpect(jsonPath("$.platformFeeAmount").value(30000))
                .andExpect(jsonPath("$.scheduledSettlementAmount").value(120000));

        mockMvc.perform(get("/api/settlements/monthly")
                        .param("creatorId", "creator-1")
                        .param("yearMonth", "2025-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settlementId").value("settlement-creator-1-2025-03"))
                .andExpect(jsonPath("$.settlementStatus").value("PENDING"));

        createMarchSettlement()
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Settlement already exists: creator-1 2025-03"));
    }

    @Test
    void movesSettlementFromPendingToConfirmedToPaid() throws Exception {
        createMarchSettlement().andExpect(status().isCreated());

        mockMvc.perform(patch("/api/settlements/settlement-creator-1-2025-03/confirm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"))
                .andExpect(jsonPath("$.confirmedAt").exists());

        mockMvc.perform(patch("/api/settlements/settlement-creator-1-2025-03/pay"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PAID"))
                .andExpect(jsonPath("$.paidAt").exists());
    }

    @Test
    void rejectsPayBeforeConfirm() throws Exception {
        createMarchSettlement().andExpect(status().isCreated());

        mockMvc.perform(patch("/api/settlements/settlement-creator-1-2025-03/pay"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only CONFIRMED settlement can be paid."));
    }

    @Test
    void appliesFeeRateHistoryBySettlementPeriod() throws Exception {
        createFeeRate("2025-01-01", 10)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.feeRatePercentage").value(10));

        mockMvc.perform(get("/api/settlements/monthly")
                        .param("creatorId", "creator-1")
                        .param("yearMonth", "2025-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.feeRatePercentage").value(10))
                .andExpect(jsonPath("$.platformFeeAmount").value(15000))
                .andExpect(jsonPath("$.scheduledSettlementAmount").value(135000));
    }

    @Test
    void settlementSnapshotKeepsOriginalFeeRateAfterRateChanges() throws Exception {
        createFeeRate("2025-01-01", 10).andExpect(status().isCreated());
        createMarchSettlement()
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.feeRatePercentage").value(10));
        createFeeRate("2025-02-01", 30).andExpect(status().isCreated());

        mockMvc.perform(get("/api/settlements/monthly")
                        .param("creatorId", "creator-1")
                        .param("yearMonth", "2025-03"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settlementStatus").value("PENDING"))
                .andExpect(jsonPath("$.feeRatePercentage").value(10))
                .andExpect(jsonPath("$.platformFeeAmount").value(15000));
    }

    @Test
    void downloadsAdminSettlementCsv() throws Exception {
        mockMvc.perform(get("/api/admin/settlements.csv")
                        .param("startDate", "2025-03-01")
                        .param("endDate", "2025-03-31"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, containsString("settlements-2025-03-01-2025-03-31.csv")))
                .andExpect(content().string(containsString("creatorId,creatorName,totalSaleAmount")))
                .andExpect(content().string(containsString("\"creator-1\",\"김강사\",260000,110000,150000,30000,120000,4,2,20")))
                .andExpect(content().string(containsString("\"TOTAL\",\"\",320000,110000,210000,42000,168000,0,0,0")));
    }

    private org.springframework.test.web.servlet.ResultActions createMarchSettlement() throws Exception {
        return mockMvc.perform(post("/api/settlements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "creatorId", "creator-1",
                        "yearMonth", "2025-03"
                ))));
    }

    private org.springframework.test.web.servlet.ResultActions createFeeRate(
            String effectiveFrom,
            int feeRatePercentage
    ) throws Exception {
        return mockMvc.perform(post("/api/admin/fee-rates")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "effectiveFrom", effectiveFrom,
                        "feeRatePercentage", feeRatePercentage
                ))));
    }
}
