package com.bf.navigator.service.station.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;

import com.bf.navigator.service.station.client.StationDataClient;
import com.bf.navigator.service.station.client.StationFacilitiesClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK, properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration," +
                "org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration," +
                "org.springframework.boot.autoconfigure.jdbc.DataJpaRepositoriesAutoConfiguration," +
                "org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration"
})
@AutoConfigureMockMvc
@DisplayName("StationController Integration Tests")
class StationControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private StationDataClient stationDataClient;

    @MockBean
    private StationFacilitiesClient stationFacilitiesClient;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("searchStations returns stations for valid query")
    void testSearchStations() throws Exception {
        // Mock client to return realistic ArrayNode matching service mapping
        ArrayNode stationsArray = objectMapper.createArrayNode();
        ObjectNode stationNode = objectMapper.createObjectNode();
        stationNode.put("name", "Test");
        stationNode.put("number", "1");
        stationNode.put("evaNumber", "1");
        stationNode.put("city", "Test");
        stationsArray.add(stationNode);

        when(stationDataClient.searchStations("test")).thenReturn(stationsArray);

        mockMvc.perform(get("/stations/search")
                .param("query", "test")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name", is("Test")))
                .andExpect(jsonPath("$[0].number", is(1)))
                .andExpect(jsonPath("$[0].evaNumber", is(1)))
                .andExpect(jsonPath("$[0].city", is("Test")));
    }

    @Test
    @DisplayName("getStation returns station for valid id")
    void testGetStation() throws Exception {
        // Mock client to return JsonNode for Hamburg Hbf
        ObjectNode stationNode = objectMapper.createObjectNode();
        stationNode.put("name", "Hamburg Hbf");
        stationNode.put("number", "2514");
        stationNode.put("evaNumber", "8002549");
        stationNode.put("city", "Hamburg");
        stationNode.put("category", 2);
        stationNode.put("hasSteplessAccess", "yes");
        stationNode.put("hasMobilityService", "yes");
        stationNode.put("hasWiFi", true);

        when(stationDataClient.getStationById(2514L)).thenReturn(stationNode);

        mockMvc.perform(get("/stations/2514")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name", is("Hamburg Hbf")))
                .andExpect(jsonPath("$.number", is(2514)))
                .andExpect(jsonPath("$.evaNumber", is(8002549)))
                .andExpect(jsonPath("$.city", is("Hamburg")))
                .andExpect(jsonPath("$.category", is(2)))
                .andExpect(jsonPath("$.hasSteplessAccess", is("yes")))
                .andExpect(jsonPath("$.hasMobilityService", is("yes")))
                .andExpect(jsonPath("$.hasWiFi", is(true)));
    }

    @Test
    @DisplayName("getStationFacilities returns facilities for valid stationNumber")
    void testGetStationFacilities() throws Exception {
        // Mock client to return ArrayNode with 3 facilities
        ArrayNode facilitiesArray = objectMapper.createArrayNode();
        ObjectNode f1 = objectMapper.createObjectNode();
        f1.put("description", "zu Gleis 1/2");
        facilitiesArray.add(f1);

        ObjectNode f2 = objectMapper.createObjectNode();
        f2.put("description", "zu Gleis 3/4");
        facilitiesArray.add(f2);

        ObjectNode f3 = objectMapper.createObjectNode();
        f3.put("description", "zu Gleis 5/6");
        facilitiesArray.add(f3);

        when(stationFacilitiesClient.getStationWithFacilitiesJson(53L)).thenReturn(facilitiesArray);

        mockMvc.perform(get("/stations/53/facilities")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("[0].description", is("zu Gleis 1/2")))
                .andExpect(jsonPath("[1].description", is("zu Gleis 3/4")))
                .andExpect(jsonPath("[2].description", is("zu Gleis 5/6")));
    }

    @Test
    @DisplayName("searchStations returns empty list when no results")
    void testSearchStationsNoResults() throws Exception {
        // Negative test: mock empty/nil result
        when(stationDataClient.searchStations(anyString())).thenReturn(null);

        mockMvc.perform(get("/stations/search")
                .param("query", "nonexistent")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }
}
