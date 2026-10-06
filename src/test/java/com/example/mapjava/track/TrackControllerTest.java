package com.example.mapjava.track;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
class TrackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void uploadsPointsAndMergesThemByDay() throws Exception {
        String token = login("13200132000");

        MvcResult firstUpload = mockMvc.perform(post("/api/tracks/upload")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  {
                                    "latitude": 31.2304,
                                    "longitude": 121.4737,
                                    "recordedAt": "2026-09-19T08:00:00Z",
                                    "address": "人民广场",
                                    "accuracy": 12.5,
                                    "speed": 1.2
                                  },
                                  {
                                    "latitude": 31.2310,
                                    "longitude": 121.4742,
                                    "recordedAt": "2026-09-19T08:05:00Z",
                                    "address": "南京东路",
                                    "accuracy": 10.0,
                                    "speed": 1.4
                                  }
                                ]
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadedCount").value(2))
                .andExpect(jsonPath("$.affectedTracks", hasSize(1)))
                .andExpect(jsonPath("$.affectedTracks[0].date").value("2026-09-19"))
                .andReturn();

        String trackId = json(firstUpload).at("/affectedTracks/0/trackId").asText();

        mockMvc.perform(post("/api/tracks/upload")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  {
                                    "latitude": 31.2320,
                                    "longitude": 121.4750,
                                    "recordedAt": "2026-09-19T08:10:00Z",
                                    "address": "外滩"
                                  }
                                ]
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadedCount").value(1))
                .andExpect(jsonPath("$.affectedTracks[0].trackId").value(trackId));

        mockMvc.perform(get("/api/me/tracks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].trackId").value(trackId))
                .andExpect(jsonPath("$[0].date").value("2026-09-19"))
                .andExpect(jsonPath("$[0].startAddress").value("人民广场"))
                .andExpect(jsonPath("$[0].endAddress").value("外滩"))
                .andExpect(jsonPath("$[0].startedAt").doesNotExist())
                .andExpect(jsonPath("$[0].endedAt").doesNotExist())
                .andExpect(jsonPath("$[0].distanceMeters").doesNotExist())
                .andExpect(jsonPath("$[0].pointCount").doesNotExist());

        mockMvc.perform(get("/api/tracks/" + trackId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.trackId").value(trackId))
                .andExpect(jsonPath("$.startAddress").value("人民广场"))
                .andExpect(jsonPath("$.endAddress").value("外滩"))
                .andExpect(jsonPath("$.pointCount").value(3))
                .andExpect(jsonPath("$.points", hasSize(3)))
                .andExpect(jsonPath("$.points[0].address").value("人民广场"))
                .andExpect(jsonPath("$.points[2].address").value("外滩"));
    }

    @Test
    void splitsUploadAcrossDifferentDays() throws Exception {
        String token = login("13300133000");

        mockMvc.perform(post("/api/tracks/upload")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  {
                                    "latitude": 31.2304,
                                    "longitude": 121.4737,
                                    "recordedAt": "2026-09-19T08:00:00Z",
                                    "address": "第一天"
                                  },
                                  {
                                    "latitude": 31.2404,
                                    "longitude": 121.4837,
                                    "recordedAt": "2026-09-20T08:00:00Z",
                                    "address": "第二天"
                                  }
                                ]
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.uploadedCount").value(2))
                .andExpect(jsonPath("$.affectedTracks", hasSize(2)));

        mockMvc.perform(get("/api/me/tracks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].date").value("2026-09-20"))
                .andExpect(jsonPath("$[1].date").value("2026-09-19"));
    }

    @Test
    void rejectsTrackAccessFromAnotherUser() throws Exception {
        String firstToken = login("13100131000");
        String secondToken = login("13000130000");

        MvcResult upload = mockMvc.perform(post("/api/tracks/upload")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + firstToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  {
                                    "latitude": 31.2304,
                                    "longitude": 121.4737,
                                    "recordedAt": "2026-09-19T08:00:00Z",
                                    "address": "私有轨迹"
                                  }
                                ]
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String trackId = json(upload).at("/affectedTracks/0/trackId").asText();

        mockMvc.perform(get("/api/tracks/" + trackId)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + secondToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Track not found: " + trackId));
    }

    @Test
    void friendCanViewFriendTrackList() throws Exception {
        String ownerToken = login("16800168000");
        String friendToken = login("16900169000");

        MvcResult upload = mockMvc.perform(post("/api/tracks/upload")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  {
                                    "latitude": 31.2304,
                                    "longitude": 121.4737,
                                    "recordedAt": "2026-09-19T08:00:00Z",
                                    "address": "好友起点"
                                  },
                                  {
                                    "latitude": 31.2404,
                                    "longitude": 121.4837,
                                    "recordedAt": "2026-09-19T08:10:00Z",
                                    "address": "好友终点"
                                  }
                                ]
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String trackId = json(upload).at("/affectedTracks/0/trackId").asText();

        MvcResult request = mockMvc.perform(post("/api/friends")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + friendToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "phone": "16800168000"
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        String ownerUserId = json(request).at("/receiver/userId").asText();
        String requestId = json(request).at("/requestId").asText();

        mockMvc.perform(post("/api/friends/requests/" + requestId + "/accept")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/friends/" + ownerUserId + "/tracks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + friendToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].trackId").value(trackId))
                .andExpect(jsonPath("$[0].date").value("2026-09-19"))
                .andExpect(jsonPath("$[0].startAddress").value("好友起点"))
                .andExpect(jsonPath("$[0].endAddress").value("好友终点"));
    }

    @Test
    void rejectsFriendTrackListForNonFriend() throws Exception {
        String ownerToken = login("17000170000");
        String strangerToken = login("17100171000");

        mockMvc.perform(post("/api/tracks/upload")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                [
                                  {
                                    "latitude": 31.2304,
                                    "longitude": 121.4737,
                                    "recordedAt": "2026-09-19T08:00:00Z",
                                    "address": "非好友不可见"
                                  }
                                ]
                                """))
                .andExpect(status().isOk());

        MvcResult ownerProfile = mockMvc.perform(get("/api/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + ownerToken))
                .andExpect(status().isOk())
                .andReturn();

        String ownerUserId = json(ownerProfile).at("/userId").asText();

        mockMvc.perform(get("/api/friends/" + ownerUserId + "/tracks")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + strangerToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("好友不存在"));
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
