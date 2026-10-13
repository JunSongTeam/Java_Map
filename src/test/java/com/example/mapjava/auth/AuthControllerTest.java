package com.example.mapjava.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest(properties = "app.avatar.storage-dir=target/test-avatars")
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
    void updatesCurrentUserProfile() throws Exception {
        String phone = "13900139000";
        String token = tokenFrom(login(phone, sendCode(phone)));

        mockMvc.perform(post("/api/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "小明",
                                  "avatarUrl": "https://example.com/avatar.png"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value(phone))
                .andExpect(jsonPath("$.nickname").value("小明"))
                .andExpect(jsonPath("$.avatarUrl").value("https://example.com/avatar.png"))
                .andExpect(jsonPath("$.memberStatus").value("inactive"))
                .andExpect(jsonPath("$.memberExpiresAt").value(nullValue()));

        mockMvc.perform(get("/api/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value(phone))
                .andExpect(jsonPath("$.nickname").value("小明"))
                .andExpect(jsonPath("$.avatarUrl").value("https://example.com/avatar.png"));
    }

    @Test
    void rejectsBlankProfileFields() throws Exception {
        String token = tokenFrom(login("13900139001", sendCode("13900139001")));

        mockMvc.perform(post("/api/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": " "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("昵称不能为空"));
    }

    @Test
    void uploadsCurrentUserAvatar() throws Exception {
        String phone = "13900139002";
        String token = tokenFrom(login(phone, sendCode(phone)));
        byte[] avatarBytes = "fake png content".getBytes(StandardCharsets.UTF_8);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                MediaType.IMAGE_PNG_VALUE,
                avatarBytes
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/me/avatar")
                        .file(file)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value(phone))
                .andExpect(jsonPath("$.avatarUrl", matchesPattern("^/api/users/.+/avatar$")))
                .andReturn();

        String avatarUrl = fieldFrom(uploadResult, "avatarUrl");
        mockMvc.perform(get(avatarUrl))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_PNG))
                .andExpect(content().bytes(avatarBytes));
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
