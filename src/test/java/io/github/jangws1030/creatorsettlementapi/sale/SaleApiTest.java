package io.github.jangws1030.creatorsettlementapi.sale;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.jangws1030.creatorsettlementapi.sale.api.SaleApiDtos;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@Transactional
@SpringBootTest
@AutoConfigureMockMvc
class SaleApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listsCreatorSalesWithinDateRange() throws Exception {
        mockMvc.perform(get("/api/sales")
                        .param("creatorId", "creator-1")
                        .param("fromDate", "2025-03-01")
                        .param("toDate", "2025-03-31"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.creatorId").value("creator-1"))
                .andExpect(jsonPath("$.creatorName").value("김강사"))
                .andExpect(jsonPath("$.totalSaleAmount").value(260000))
                .andExpect(jsonPath("$.totalRefundAmount").value(110000))
                .andExpect(jsonPath("$.itemCount").value(4))
                .andExpect(jsonPath("$.items[0].id").value("sale-4"))
                .andExpect(jsonPath("$.items[0].saleStatus").value("PARTIALLY_REFUNDED"))
                .andExpect(jsonPath("$.items[1].saleStatus").value("FULLY_REFUNDED"));
    }

    @Test
    void listsSalesWithAllLinkedCancellationsEvenWhenCancellationIsOutsideSaleDateRange() throws Exception {
        mockMvc.perform(get("/api/sales")
                        .param("creatorId", "creator-1")
                        .param("fromDate", "2025-04-01")
                        .param("toDate", "2025-04-30"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalSaleAmount").value(190000))
                .andExpect(jsonPath("$.totalRefundAmount").value(140000))
                .andExpect(jsonPath("$.itemCount").value(2))
                .andExpect(jsonPath("$.items[0].id").value("sale-9"))
                .andExpect(jsonPath("$.items[0].saleStatus").value("FULLY_REFUNDED"))
                .andExpect(jsonPath("$.items[0].cancellations[0].id").value("cancel-6"))
                .andExpect(jsonPath("$.items[1].id").value("sale-8"))
                .andExpect(jsonPath("$.items[1].refundedAmount").value(50000))
                .andExpect(jsonPath("$.items[1].cancellations.length()").value(2));
    }

    @Test
    void createsSale() throws Exception {
        SaleApiDtos.CreateSaleRequest request = new SaleApiDtos.CreateSaleRequest(
                "sale-extra-1",
                "course-1",
                "student-99",
                70_000L,
                java.time.OffsetDateTime.parse("2025-03-25T10:00:00+09:00")
        );

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("sale-extra-1"))
                .andExpect(jsonPath("$.creatorId").value("creator-1"))
                .andExpect(jsonPath("$.courseId").value("course-1"))
                .andExpect(jsonPath("$.studentId").value("student-99"))
                .andExpect(jsonPath("$.amount").value(70000));
    }

    @Test
    void rejectsDuplicateSaleId() throws Exception {
        SaleApiDtos.CreateSaleRequest request = new SaleApiDtos.CreateSaleRequest(
                "sale-1",
                "course-1",
                "student-99",
                70_000L,
                java.time.OffsetDateTime.parse("2025-03-25T10:00:00+09:00")
        );

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Sale already exists: sale-1"));
    }

    @Test
    void rejectsSaleWhenCourseDoesNotExist() throws Exception {
        SaleApiDtos.CreateSaleRequest request = new SaleApiDtos.CreateSaleRequest(
                "sale-missing-course",
                "course-missing",
                "student-99",
                70_000L,
                java.time.OffsetDateTime.parse("2025-03-25T10:00:00+09:00")
        );

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Course not found: course-missing"));
    }

    @Test
    void rejectsSaleWhenAmountIsNotPositive() throws Exception {
        SaleApiDtos.CreateSaleRequest request = new SaleApiDtos.CreateSaleRequest(
                "sale-zero-amount",
                "course-1",
                "student-99",
                0L,
                java.time.OffsetDateTime.parse("2025-03-25T10:00:00+09:00")
        );

        mockMvc.perform(post("/api/sales")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("amount: amount must be positive."));
    }

    @Test
    void rejectsSaleListWhenDateRangeIsInvalid() throws Exception {
        mockMvc.perform(get("/api/sales")
                        .param("creatorId", "creator-1")
                        .param("fromDate", "2025-03-31")
                        .param("toDate", "2025-03-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("fromDate must be before or equal to toDate."));
    }

    @Test
    void rejectsSaleListWhenCreatorIdIsMissing() throws Exception {
        mockMvc.perform(get("/api/sales"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Missing request parameter: creatorId"));
    }

    @Test
    void createsCancellation() throws Exception {
        SaleApiDtos.CreateCancellationRequest request = new SaleApiDtos.CreateCancellationRequest(
                "cancel-extra-1",
                10_000L,
                java.time.OffsetDateTime.parse("2025-03-16T10:00:00+09:00")
        );

        mockMvc.perform(post("/api/sales/sale-2/cancellations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("cancel-extra-1"))
                .andExpect(jsonPath("$.saleId").value("sale-2"))
                .andExpect(jsonPath("$.refundAmount").value(10000))
                .andExpect(jsonPath("$.accumulatedRefundAmount").value(10000))
                .andExpect(jsonPath("$.remainingSaleAmount").value(40000));
    }

    @Test
    void createsCancellationWhenRefundEqualsRemainingSaleAmount() throws Exception {
        SaleApiDtos.CreateCancellationRequest request = new SaleApiDtos.CreateCancellationRequest(
                "cancel-full-extra-1",
                50_000L,
                java.time.OffsetDateTime.parse("2025-03-16T10:00:00+09:00")
        );

        mockMvc.perform(post("/api/sales/sale-2/cancellations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accumulatedRefundAmount").value(50000))
                .andExpect(jsonPath("$.remainingSaleAmount").value(0));
    }

    @Test
    void rejectsCancellationWhenRefundAmountIsNotPositive() throws Exception {
        SaleApiDtos.CreateCancellationRequest request = new SaleApiDtos.CreateCancellationRequest(
                "cancel-zero-amount",
                0L,
                java.time.OffsetDateTime.parse("2025-03-16T10:00:00+09:00")
        );

        mockMvc.perform(post("/api/sales/sale-2/cancellations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("refundAmount: refundAmount must be positive."));
    }

    @Test
    void rejectsDuplicateCancellationId() throws Exception {
        SaleApiDtos.CreateCancellationRequest request = new SaleApiDtos.CreateCancellationRequest(
                "cancel-1",
                10_000L,
                java.time.OffsetDateTime.parse("2025-03-16T10:00:00+09:00")
        );

        mockMvc.perform(post("/api/sales/sale-2/cancellations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cancellation already exists: cancel-1"));
    }

    @Test
    void rejectsCancellationWhenSaleDoesNotExist() throws Exception {
        SaleApiDtos.CreateCancellationRequest request = new SaleApiDtos.CreateCancellationRequest(
                "cancel-missing-sale",
                10_000L,
                java.time.OffsetDateTime.parse("2025-03-16T10:00:00+09:00")
        );

        mockMvc.perform(post("/api/sales/sale-missing/cancellations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Sale not found: sale-missing"));
    }

    @Test
    void rejectsCancellationBeforePaidAt() throws Exception {
        SaleApiDtos.CreateCancellationRequest request = new SaleApiDtos.CreateCancellationRequest(
                "cancel-before-paid-at",
                10_000L,
                java.time.OffsetDateTime.parse("2025-03-09T10:00:00+09:00")
        );

        mockMvc.perform(post("/api/sales/sale-2/cancellations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("cancelledAt must be after or equal to paidAt."));
    }

    @Test
    void rejectsCancellationWhenRefundExceedsOriginalSaleAmount() throws Exception {
        SaleApiDtos.CreateCancellationRequest request = new SaleApiDtos.CreateCancellationRequest(
                "cancel-over-1",
                60_000L,
                java.time.OffsetDateTime.parse("2025-03-30T11:00:00+09:00")
        );

        mockMvc.perform(post("/api/sales/sale-4/cancellations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Refund amount exceeds original sale amount."));
    }
}
