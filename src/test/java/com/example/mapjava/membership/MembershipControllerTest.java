package com.example.mapjava.membership;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class MembershipControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listsProductsCreatesOrderAndVerifiesMembership() throws Exception {
        String token = login("17200172000");

        mockMvc.perform(get("/api/membership/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[0].productId").value("20231004"))
                .andExpect(jsonPath("$[0].title").value("永久会员"))
                .andExpect(jsonPath("$[0].durationDays").value(36500))
                .andExpect(jsonPath("$[0].priceCents").value(16800))
                .andExpect(jsonPath("$[0].currency").value("CNY"))
                .andExpect(jsonPath("$[0].appleProductId").value("location.vip.permanent"))
                .andExpect(jsonPath("$[0].time").value("永久会员"))
                .andExpect(jsonPath("$[0].price").value("¥168"))
                .andExpect(jsonPath("$[0].originalPrice").value("¥688"))
                .andExpect(jsonPath("$[0].unitPrice").value("¥0.01/天"))
                .andExpect(jsonPath("$[0].orderID").value("20231004"))
                .andExpect(jsonPath("$[0].status").value("1"));

        mockMvc.perform(get("/api/membership/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberStatus").value("inactive"))
                .andExpect(jsonPath("$.memberExpiresAt").doesNotExist())
                .andExpect(jsonPath("$.active").value(false));

        MvcResult orderResult = mockMvc.perform(post("/api/membership/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "20231004",
                                  "paymentProvider": "apple_iap"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").isNotEmpty())
                .andExpect(jsonPath("$.productId").value("20231004"))
                .andExpect(jsonPath("$.productTitle").value("永久会员"))
                .andExpect(jsonPath("$.paymentProvider").value("apple_iap"))
                .andExpect(jsonPath("$.status").value("pending"))
                .andExpect(jsonPath("$.memberStatus").value("inactive"))
                .andReturn();

        String orderId = json(orderResult).at("/orderId").asText();
        String transactionId = "ios-tx-" + UUID.randomUUID();

        mockMvc.perform(post("/api/membership/orders/verify")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "orderId": "%s",
                                  "transactionId": "%s"
                                }
                                """.formatted(orderId, transactionId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(orderId))
                .andExpect(jsonPath("$.status").value("paid"))
                .andExpect(jsonPath("$.transactionId").value(transactionId))
                .andExpect(jsonPath("$.paidAt").isNotEmpty())
                .andExpect(jsonPath("$.memberStatus").value("active"))
                .andExpect(jsonPath("$.memberExpiresAt").isNotEmpty());

        mockMvc.perform(get("/api/membership/status")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberStatus").value("active"))
                .andExpect(jsonPath("$.memberExpiresAt").isNotEmpty())
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(get("/api/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.memberStatus").value("active"))
                .andExpect(jsonPath("$.memberExpiresAt", matchesPattern("^.+Z$")));
    }

    @Test
    void rejectsUnknownProductAndMissingToken() throws Exception {
        String token = login("17300173000");

        mockMvc.perform(post("/api/membership/orders")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "missing_product"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("会员商品不存在"));

        mockMvc.perform(post("/api/membership/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productId": "20231004"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Missing bearer token"));
    }

    private String login(String phone) throws Exception {
        MvcResult codeResult = mockMvc.perform(post("/api/send/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "%s"
                                }
                                """.formatted(phone)))
                .andExpect(status().isOk())
                .andReturn();

        String code = json(codeResult).at("/devCode").asText();
        MvcResult loginResult = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "%s",
                                  "code": "%s"
                                }
                                """.formatted(phone, code)))
                .andExpect(status().isOk())
                .andReturn();

        return json(loginResult).at("/token").asText();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
