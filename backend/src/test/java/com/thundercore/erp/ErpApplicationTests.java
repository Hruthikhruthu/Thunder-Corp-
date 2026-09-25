package com.thundercore.erp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("dev")
class ErpApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void contextLoads() {
    }

    @Test
    void loginDefaultAdminReturnsJwt() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@thundercore.com","password":"Admin@123"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.role").value("SUPER_ADMIN"));
    }

    @Test
    void protectedDashboardRejectsAnonymousRequests() throws Exception {
        mockMvc.perform(get("/api/dashboard/stats"))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void protectedDashboardAcceptsJwt() throws Exception {
        String token = loginAndGetToken();

        mockMvc.perform(get("/api/dashboard/stats")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalUsers").exists());
    }

    @Test
    void publicRegistrationIsNotAllowed() throws Exception {
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"staff@thundercore.com",
                                  "password":"staff123",
                                  "firstName":"Staff",
                                  "lastName":"User"
                                }
                                """))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void customerCrudAcceptsAdminJwt() throws Exception {
        String token = loginAndGetToken();
        String email = "crm-" + System.nanoTime() + "@example.com";

        MvcResult created = mockMvc.perform(post("/api/sales/customers")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"CRM Test Customer",
                                  "email":"%s",
                                  "phone":"555-0101",
                                  "address":"Test Street",
                                  "tier":"PREMIUM"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.name").value("CRM Test Customer"))
                .andReturn();

        Long customerId = objectMapper.readTree(created.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asLong();

        mockMvc.perform(put("/api/sales/customers/{id}", customerId)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name":"CRM Test Customer Updated",
                                  "email":"%s",
                                  "phone":"555-0102",
                                  "address":"Updated Street",
                                  "tier":"VIP"
                                }
                                """.formatted(email)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.name").value("CRM Test Customer Updated"))
                .andExpect(jsonPath("$.data.tier").value("VIP"));

        mockMvc.perform(get("/api/sales/customers/{id}", customerId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email));

        mockMvc.perform(delete("/api/sales/customers/{id}", customerId)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    void salesOrdersReturnProductForCustomerPage() throws Exception {
        String token = loginAndGetToken();
        String sku = "SKU-" + System.nanoTime();

        MvcResult productResult = mockMvc.perform(post("/api/inventory/products")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku":"%s",
                                  "name":"CRM Order Product",
                                  "category":"CRM",
                                  "unitPrice":25.50,
                                  "quantity":10,
                                  "reorderThreshold":2
                                }
                                """.formatted(sku)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn();

        Long productId = objectMapper.readTree(productResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asLong();

        MvcResult orderResult = mockMvc.perform(post("/api/sales/orders")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "customerName":"CRM Order Customer",
                                  "customerEmail":"crm-order@example.com",
                                  "quantity":2,
                                  "status":"DRAFT",
                                  "product":{"id":%d}
                                }
                                """.formatted(productId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").exists())
                .andReturn();

        Long orderId = objectMapper.readTree(orderResult.getResponse().getContentAsString())
                .path("data")
                .path("id")
                .asLong();

        MvcResult ordersResult = mockMvc.perform(get("/api/sales/orders")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode orders = objectMapper.readTree(ordersResult.getResponse().getContentAsString()).path("data");
        JsonNode createdOrder = null;
        for (JsonNode order : orders) {
            if (order.path("id").asLong() == orderId) {
                createdOrder = order;
                break;
            }
        }

        assertThat(createdOrder).isNotNull();
        assertThat(createdOrder.path("product").path("name").asText()).isEqualTo("CRM Order Product");
    }

    private String loginAndGetToken() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@thundercore.com","password":"Admin@123"}
                                """))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        return body.get("token").asText();
    }
}
