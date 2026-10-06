package com.example.mapjava.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
class VerificationCodeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void sendsSmsVerificationCode() throws Exception {
        mockMvc.perform(post("/api/send/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "13800138000"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("13800138000"))
                .andExpect(jsonPath("$.expiresInSeconds").value(300))
                .andExpect(jsonPath("$.expiresAt").isNotEmpty())
                .andExpect(jsonPath("$.devCode").value("123456"));
    }

    @Test
    void ignoresUnknownExtraFields() throws Exception {
        mockMvc.perform(post("/api/send/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "13800138000",
                                  "code": "email"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("13800138000"));
    }

    @Test
    void verifiesLatestCodeAndConsumesIt() throws Exception {
        String phone = "13900139000";
        MvcResult sendResult = mockMvc.perform(post("/api/send/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "%s"
                                }
                                """.formatted(phone)))
                .andExpect(status().isOk())
                .andReturn();

        String code = fieldFrom(sendResult, "devCode");

        mockMvc.perform(post("/api/verification-codes/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "target": "%s",
                                  "code": "%s"
                                }
                                """.formatted(phone, code)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.verified").value(true));

        mockMvc.perform(post("/api/verification-codes/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "target": "%s",
                                  "code": "%s"
                                }
                                """.formatted(phone, code)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Verification code is invalid or expired"));
    }

    @Test
    void rejectsInvalidTargetAndCode() throws Exception {
        mockMvc.perform(post("/api/send/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("phone must be a valid mainland China mobile number"));

        mockMvc.perform(post("/api/verification-codes/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "target": "13800138000",
                                  "code": "123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    private String fieldFrom(MvcResult result, String fieldName) throws Exception {
        Map<String, Object> body = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
        );
        return (String) body.get(fieldName);
    }
}
