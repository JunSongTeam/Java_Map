package com.example.mapjava.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
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
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void logsInNewUserAndReturnsNeverExpiringToken() throws Exception {
        String phone = "13600136000";
        String code = sendCode(phone);

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "%s",
                                  "code": "%s"
                                }
                                """.formatted(phone, code)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").doesNotExist())
                .andExpect(jsonPath("$.expiresInSeconds").doesNotExist())
                .andExpect(jsonPath("$.user").doesNotExist());
    }

    @Test
    void readsCurrentUserWithBearerToken() throws Exception {
        String phone = "13700137000";
        String code = sendCode(phone);

        MvcResult loginResult = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "%s",
                                  "code": "%s"
                                }
                                """.formatted(phone, code)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andReturn();

        String token = tokenFrom(loginResult);

        MvcResult meResult = mockMvc.perform(get("/api/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").isNotEmpty())
                .andExpect(jsonPath("$.phone").value(phone))
                .andExpect(jsonPath("$.nickname").value("User 7000"))
                .andExpect(jsonPath("$.avatarUrl", matchesPattern("^/api/users/.+/avatar$")))
                .andExpect(jsonPath("$.memberStatus").value("inactive"))
                .andExpect(jsonPath("$.memberExpiresAt").value(nullValue()))
                .andReturn();

        String avatarUrl = fieldFrom(meResult, "avatarUrl");
        mockMvc.perform(get(avatarUrl))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.valueOf("image/svg+xml")));
    }

    @Test
    void existingUserLoginKeepsSameUserId() throws Exception {
        String phone = "13500135000";
        String firstCode = sendCode(phone);
        MvcResult firstLogin = login(phone, firstCode);
        String firstToken = tokenFrom(firstLogin);
        String firstUserId = meFieldFrom(firstToken, "userId");

        String secondCode = sendCode(phone);
        MvcResult secondLogin = login(phone, secondCode);
        String secondToken = tokenFrom(secondLogin);

        assertThat(meFieldFrom(secondToken, "userId")).isEqualTo(firstUserId);
    }

    @Test
    void rejectsInvalidCodeAndMissingToken() throws Exception {
        String phone = "13400134000";
        String code = sendCode(phone);
        String wrongCode = "000000".equals(code) ? "000001" : "000000";

        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "%s",
                                  "code": "%s"
                                }
                                """.formatted(phone, wrongCode)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Verification code is invalid or expired"));

        mockMvc.perform(get("/api/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Missing bearer token"));
    }

    @Test
    void validatesLoginRequest() throws Exception {
        mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "",
                                  "code": "123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"));
    }

    private String tokenFrom(MvcResult result) throws Exception {
        Map<String, Object> body = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
        );
        String token = (String) body.get("token");
        assertThat(token).isNotBlank();
        return token;
    }

    private String sendCode(String phone) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/send/code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "%s"
                                }
                                """.formatted(phone)))
                .andExpect(status().isOk())
                .andReturn();

        String code = fieldFrom(result, "devCode");
        assertThat(code).isNotBlank();
        return code;
    }

    private MvcResult login(String phone, String code) throws Exception {
        return mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "%s",
                                  "code": "%s"
                                }
                                """.formatted(phone, code)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private String meFieldFrom(String token, String fieldName) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();

        Map<String, Object> body = objectMapper.readValue(
                result.getResponse().getContentAsString(),
                new TypeReference<>() {
                }
        );
        return (String) body.get(fieldName);
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
