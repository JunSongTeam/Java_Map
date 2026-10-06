package com.example.mapjava.friend;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.matchesPattern;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
class FriendControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void checksWhetherPhoneIsRegistered() throws Exception {
        String token = login("15000150000");
        login("15100151000");

        mockMvc.perform(get("/api/users/registered")
                        .param("phone", "15100151000")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("15100151000"))
                .andExpect(jsonPath("$.registered").value(true))
                .andExpect(jsonPath("$.message").value("用户已注册"))
                .andExpect(jsonPath("$.user.userId").isNotEmpty())
                .andExpect(jsonPath("$.user.phone").value("15100151000"))
                .andExpect(jsonPath("$.user.nickname").value("User 1000"))
                .andExpect(jsonPath("$.user.avatarUrl", matchesPattern("^/api/users/.+/avatar$")));

        mockMvc.perform(get("/api/users/registered")
                        .param("phone", "15200152000")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("15200152000"))
                .andExpect(jsonPath("$.registered").value(false))
                .andExpect(jsonPath("$.message").value("用户未注册"))
                .andExpect(jsonPath("$.user").doesNotExist());
    }

    @Test
    void defaultUserHasDefaultFriends() throws Exception {
        String token = login("13800138000");

        mockMvc.perform(get("/api/friends")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].phone", containsInAnyOrder("13900139000", "13700137000")))
                .andExpect(jsonPath("$[0].remark").value(""))
                .andExpect(jsonPath("$[0].alreadyFriend").value(true))
                .andExpect(jsonPath("$[1].remark").value(""))
                .andExpect(jsonPath("$[1].alreadyFriend").value(true));
    }

    @Test
    void sendsFriendRequestAndAcceptsIt() throws Exception {
        String requesterToken = login("15300153000");
        String receiverToken = login("15400154000");

        MvcResult sendResult = mockMvc.perform(post("/api/friends")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "15400154000"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").isNotEmpty())
                .andExpect(jsonPath("$.status").value("pending"))
                .andExpect(jsonPath("$.message").value("好友请求已发送"))
                .andExpect(jsonPath("$.requester.phone").value("15300153000"))
                .andExpect(jsonPath("$.receiver.phone").value("15400154000"))
                .andReturn();

        String requestId = json(sendResult).at("/requestId").asText();
        String receiverUserId = json(sendResult).at("/receiver/userId").asText();

        mockMvc.perform(post("/api/friends")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "15400154000"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.requestId").value(requestId))
                .andExpect(jsonPath("$.message").value("好友请求已发送，等待对方同意"));

        mockMvc.perform(get("/api/friends/requests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + receiverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].requestId").value(requestId))
                .andExpect(jsonPath("$[0].status").value("pending"))
                .andExpect(jsonPath("$[0].message").value("待处理"))
                .andExpect(jsonPath("$[0].requester.phone").value("15300153000"));

        mockMvc.perform(get("/api/friends/requests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + requesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/friends")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + requesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(post("/api/friends/requests/" + requestId + "/accept")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + requesterToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("好友请求不存在"));

        MvcResult acceptResult = mockMvc.perform(post("/api/friends/requests/" + requestId + "/accept")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + receiverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.friendUserId").isNotEmpty())
                .andExpect(jsonPath("$.phone").value("15300153000"))
                .andExpect(jsonPath("$.nickname").value("User 3000"))
                .andExpect(jsonPath("$.remark").value(""))
                .andExpect(jsonPath("$.avatarUrl", matchesPattern("^/api/users/.+/avatar$")))
                .andExpect(jsonPath("$.alreadyFriend").value(false))
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn();

        String requesterUserId = json(acceptResult).at("/friendUserId").asText();

        mockMvc.perform(get("/api/friends")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + requesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].phone").value("15400154000"))
                .andExpect(jsonPath("$[0].nickname").value("User 4000"))
                .andExpect(jsonPath("$[0].remark").value(""))
                .andExpect(jsonPath("$[0].alreadyFriend").value(true));

        mockMvc.perform(get("/api/friends")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + receiverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].phone").value("15300153000"))
                .andExpect(jsonPath("$[0].nickname").value("User 3000"))
                .andExpect(jsonPath("$[0].remark").value(""))
                .andExpect(jsonPath("$[0].alreadyFriend").value(true));

        mockMvc.perform(put("/api/friends/" + receiverUserId + "/remark")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "remark": "徒步搭子"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.friendUserId").value(receiverUserId))
                .andExpect(jsonPath("$.phone").value("15400154000"))
                .andExpect(jsonPath("$.remark").value("徒步搭子"));

        mockMvc.perform(get("/api/friends")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + requesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].remark").value("徒步搭子"));

        mockMvc.perform(get("/api/friends")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + receiverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].friendUserId").value(requesterUserId))
                .andExpect(jsonPath("$[0].remark").value(""));

        mockMvc.perform(get("/api/friends/requests")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + receiverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(post("/api/friends")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "15400154000"
                                }
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("你们已经是好友"));

        mockMvc.perform(delete("/api/friends/" + receiverUserId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + requesterToken))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/friends")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + requesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(get("/api/friends")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + receiverToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        mockMvc.perform(delete("/api/friends/" + receiverUserId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + requesterToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("好友不存在"));
    }

    @Test
    void rejectsAddingSelfAndUnregisteredPhone() throws Exception {
        String token = login("15500155000");

        mockMvc.perform(post("/api/friends")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "15500155000"
                                }
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("不能添加自己为好友"));

        mockMvc.perform(post("/api/friends")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "15600156000"
                                }
                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("该手机号还没有注册"));
    }

    private String login(String phone) throws Exception {
        String code = sendCode(phone);
        MvcResult result = mockMvc.perform(post("/api/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "%s",
                                  "code": "%s"
                                }
                                """.formatted(phone, code)))
                .andExpect(status().isOk())
                .andReturn();

        String token = json(result).at("/token").asText();
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

        String code = json(result).at("/devCode").asText();
        assertThat(code).isNotBlank();
        return code;
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
