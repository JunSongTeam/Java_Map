package com.example.mapjava.systemmessage;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

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
class SystemMessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void listsDefaultAnnouncementAndMarksItRead() throws Exception {
        String token = login("16600166000");

        mockMvc.perform(get("/api/system-messages/unread-count")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1));

        JsonNode messages = messages(token);
        JsonNode announcement = findByType(messages, "system_announcement")
                .orElseThrow();
        assertThat(announcement.at("/title").asText()).isEqualTo("系统公告");
        assertThat(announcement.at("/read").asBoolean()).isFalse();

        mockMvc.perform(put("/api/system-messages/" + announcement.at("/messageId").asText() + "/read")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("system_announcement"))
                .andExpect(jsonPath("$.read").value(true))
                .andExpect(jsonPath("$.readAt").isNotEmpty());

        mockMvc.perform(get("/api/system-messages/unread-count")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));
    }

    @Test
    void friendActionsCreateSystemMessages() throws Exception {
        String requesterToken = login("15700157000");
        String receiverToken = login("15800158000");

        MvcResult requestResult = mockMvc.perform(post("/api/friends")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + requesterToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "15800158000"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String requestId = json(requestResult).at("/requestId").asText();

        JsonNode receiverMessages = messages(receiverToken);
        JsonNode friendRequestMessage = findByType(receiverMessages, "friend_request")
                .orElseThrow();
        assertThat(friendRequestMessage.at("/title").asText()).isEqualTo("好友请求");
        assertThat(friendRequestMessage.at("/payload/requestId").asText()).isEqualTo(requestId);
        assertThat(friendRequestMessage.at("/payload/fromPhone").asText()).isEqualTo("15700157000");

        mockMvc.perform(post("/api/friends/requests/" + requestId + "/accept")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + receiverToken))
                .andExpect(status().isOk());

        JsonNode requesterMessages = messages(requesterToken);
        JsonNode acceptedMessage = findByType(requesterMessages, "friend_accepted")
                .orElseThrow();
        assertThat(acceptedMessage.at("/title").asText()).isEqualTo("好友已同意");
        assertThat(acceptedMessage.at("/payload/requestId").asText()).isEqualTo(requestId);
        assertThat(acceptedMessage.at("/payload/friendPhone").asText()).isEqualTo("15800158000");

        mockMvc.perform(put("/api/system-messages/read-all")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + requesterToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(0));
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

    private JsonNode messages(String token) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/system-messages")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return json(result);
    }

    private Optional<JsonNode> findByType(JsonNode messages, String type) {
        for (JsonNode message : messages) {
            if (type.equals(message.at("/type").asText())) {
                return Optional.of(message);
            }
        }
        return Optional.empty();
    }

    private JsonNode json(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }
}
