package com.example.mapjava.place;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PlaceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void createsAndListsPlaces() throws Exception {
        String requestBody = """
                {
                  "name": "People's Square",
                  "category": "landmark",
                  "latitude": 31.2304,
                  "longitude": 121.4737,
                  "description": "Central Shanghai"
                }
                """;

        mockMvc.perform(post("/api/places")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.name").value("People's Square"))
                .andExpect(jsonPath("$.category").value("landmark"));

        mockMvc.perform(get("/api/places")
                        .param("category", "landmark"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("People's Square"));
    }

    @Test
    void rejectsInvalidCoordinates() throws Exception {
        String requestBody = """
                {
                  "name": "Bad Point",
                  "category": "debug",
                  "latitude": 120.0,
                  "longitude": 200.0,
                  "description": ""
                }
                """;

        mockMvc.perform(post("/api/places")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details", hasSize(2)));
    }

    @Test
    void returnsNotFoundForMissingPlace() throws Exception {
        mockMvc.perform(delete("/api/places/00000000-0000-0000-0000-000000000000"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Place not found: 00000000-0000-0000-0000-000000000000"));
    }
}
