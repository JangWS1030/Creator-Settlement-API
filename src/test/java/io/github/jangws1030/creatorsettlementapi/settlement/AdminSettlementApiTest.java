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
class AdminSettlementApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void aggregatesCreatorSettlementsForPeriod() throws Exception {
        mockMvc.perform(get("/api/admin/settlements")
                        .param("startDate", "2025-03-01")
                        .param("endDate", "2025-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSaleAmount").value(320000))
                .andExpect(jsonPath("$.totalRefundAmount").value(110000))
                .andExpect(jsonPath("$.totalNetSaleAmount").value(210000))
                .andExpect(jsonPath("$.totalPlatformFeeAmount").value(42000))
                .andExpect(jsonPath("$.totalScheduledSettlementAmount").value(168000))
                .andExpect(jsonPath("$.creatorSettlements.length()").value(6))
                .andExpect(jsonPath("$.creatorSettlements[0].creatorId").value("creator-1"))
                .andExpect(jsonPath("$.creatorSettlements[0].scheduledSettlementAmount").value(120000))
                .andExpect(jsonPath("$.creatorSettlements[1].creatorId").value("creator-2"))
                .andExpect(jsonPath("$.creatorSettlements[1].scheduledSettlementAmount").value(48000))
                .andExpect(jsonPath("$.creatorSettlements[2].creatorId").value("creator-3"))
                .andExpect(jsonPath("$.creatorSettlements[2].scheduledSettlementAmount").value(0))
                .andExpect(jsonPath("$.creatorSettlements[3].creatorId").value("creator-4"))
                .andExpect(jsonPath("$.creatorSettlements[3].scheduledSettlementAmount").value(0))
                .andExpect(jsonPath("$.creatorSettlements[4].creatorId").value("creator-5"))
                .andExpect(jsonPath("$.creatorSettlements[4].scheduledSettlementAmount").value(0))
                .andExpect(jsonPath("$.creatorSettlements[5].creatorId").value("creator-6"))
                .andExpect(jsonPath("$.creatorSettlements[5].scheduledSettlementAmount").value(0));
    }

    @Test
    void aggregatesAdditionalAprilScenariosForAllCreators() throws Exception {
        mockMvc.perform(get("/api/admin/settlements")
                        .param("startDate", "2025-04-01")
                        .param("endDate", "2025-04-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSaleAmount").value(230000))
                .andExpect(jsonPath("$.totalRefundAmount").value(90000))
                .andExpect(jsonPath("$.totalNetSaleAmount").value(140000))
                .andExpect(jsonPath("$.totalPlatformFeeAmount").value(28000))
                .andExpect(jsonPath("$.totalScheduledSettlementAmount").value(112000))
                .andExpect(jsonPath("$.creatorSettlements.length()").value(6))
                .andExpect(jsonPath("$.creatorSettlements[0].creatorId").value("creator-1"))
                .andExpect(jsonPath("$.creatorSettlements[0].scheduledSettlementAmount").value(112000))
                .andExpect(jsonPath("$.creatorSettlements[1].creatorId").value("creator-2"))
                .andExpect(jsonPath("$.creatorSettlements[1].scheduledSettlementAmount").value(0))
                .andExpect(jsonPath("$.creatorSettlements[2].creatorId").value("creator-3"))
                .andExpect(jsonPath("$.creatorSettlements[2].scheduledSettlementAmount").value(0))
                .andExpect(jsonPath("$.creatorSettlements[3].creatorId").value("creator-4"))
                .andExpect(jsonPath("$.creatorSettlements[3].scheduledSettlementAmount").value(0))
                .andExpect(jsonPath("$.creatorSettlements[4].creatorId").value("creator-5"))
                .andExpect(jsonPath("$.creatorSettlements[4].scheduledSettlementAmount").value(0))
                .andExpect(jsonPath("$.creatorSettlements[5].creatorId").value("creator-6"))
                .andExpect(jsonPath("$.creatorSettlements[5].scheduledSettlementAmount").value(0));
    }

    @Test
    void rejectsInvalidDateRange() throws Exception {
        mockMvc.perform(get("/api/admin/settlements")
                        .param("startDate", "2025-03-31")
                        .param("endDate", "2025-03-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("startDate must be before or equal to endDate."));
    }

    @Test
    void rejectsAdminSettlementWhenStartDateIsMissing() throws Exception {
        mockMvc.perform(get("/api/admin/settlements")
                        .param("endDate", "2025-03-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing request parameter: startDate"));
    }
}
