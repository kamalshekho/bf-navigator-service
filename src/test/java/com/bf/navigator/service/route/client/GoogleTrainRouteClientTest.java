package com.bf.navigator.service.route.client;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.bf.navigator.service.route.dto.TrainRouteRequestDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

class GoogleTrainRouteClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void computeTrainRoutesReturnsRealisticMultipleRoutesInDebugMode() {
        GoogleTrainRouteClient client = new GoogleTrainRouteClient(new RestTemplate(), objectMapper, "");
        TrainRouteRequestDTO request = TrainRouteRequestDTO.builder()
                .origin("Hamburg Hbf")
                .destination("Braunschweig Hbf")
                .departureTime(OffsetDateTime.parse("2026-04-02T08:00:00Z"))
                .build();

        ArrayNode routes = client.computeTrainRoutes(request, true);

        assertEquals(3, routes.size());
        assertEquals(1, countTransitSteps(routes.get(0)));
        assertEquals(2, countTransitSteps(routes.get(1)));
        assertEquals(3, countTransitSteps(routes.get(2)));
    }

    @Test
    void computeTrainRoutesDebugPayloadMarksTransitStepsWithTransitTravelMode() {
        GoogleTrainRouteClient client = new GoogleTrainRouteClient(new RestTemplate(), objectMapper, "");
        TrainRouteRequestDTO request = TrainRouteRequestDTO.builder()
                .origin("Hamburg Hbf")
                .destination("Braunschweig Hbf")
                .departureTime(OffsetDateTime.parse("2026-04-02T08:00:00Z"))
                .build();

        ArrayNode routes = client.computeTrainRoutes(request, true);

        for (JsonNode route : routes) {
            for (JsonNode leg : route.path("legs")) {
                for (JsonNode step : leg.path("steps")) {
                    if (!step.path("transitDetails").isMissingNode()) {
                        assertEquals("TRANSIT", step.path("travelMode").asText());
                    }
                }
            }
        }
    }

    @Test
    void computeTrainRoutesRequestsAlternativeRoutesForRealApiCalls() {
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        GoogleTrainRouteClient client = new GoogleTrainRouteClient(restTemplate, objectMapper, "test-api-key");
        TrainRouteRequestDTO request = TrainRouteRequestDTO.builder()
                .origin("Hamburg Hbf")
                .destination("Braunschweig Hbf")
                .departureTime(OffsetDateTime.parse("2026-04-02T08:00:00Z"))
                .build();

        when(restTemplate.exchange(
                eq("https://routes.googleapis.com/directions/v2:computeRoutes"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)))
                        .thenReturn(ResponseEntity.ok("{\"routes\":[]}"));

        client.computeTrainRoutes(request, false);

        ArgumentCaptor<HttpEntity<String>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq("https://routes.googleapis.com/directions/v2:computeRoutes"),
                eq(HttpMethod.POST),
                entityCaptor.capture(),
                eq(String.class));

        String requestBody = entityCaptor.getValue().getBody();
        assertTrue(requestBody.contains("\"computeAlternativeRoutes\":true"));
    }

    @Test
    void computeTrainRoutesRequestsFullGooglePayloadForLiveCalls() {
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        GoogleTrainRouteClient client = new GoogleTrainRouteClient(restTemplate, objectMapper, "test-api-key");
        TrainRouteRequestDTO request = TrainRouteRequestDTO.builder()
                .origin("Hamburg Hbf")
                .destination("Braunschweig Hbf")
                .departureTime(OffsetDateTime.parse("2026-04-02T08:00:00Z"))
                .build();

        when(restTemplate.exchange(
                eq("https://routes.googleapis.com/directions/v2:computeRoutes"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)))
                        .thenReturn(ResponseEntity.ok("{\"routes\":[]}"));

        client.computeTrainRoutes(request, false);

        ArgumentCaptor<HttpEntity<String>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).exchange(
                eq("https://routes.googleapis.com/directions/v2:computeRoutes"),
                eq(HttpMethod.POST),
                entityCaptor.capture(),
                eq(String.class));

        assertEquals("*", entityCaptor.getValue().getHeaders().getFirst("X-Goog-FieldMask"));
    }

    @Test
    void computeTrainRoutesParsesRoutesFromFullLivePayloadShape() {
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        GoogleTrainRouteClient client = new GoogleTrainRouteClient(restTemplate, objectMapper, "test-api-key");
        TrainRouteRequestDTO request = TrainRouteRequestDTO.builder()
                .origin("Hamburg Hbf")
                .destination("Braunschweig Hbf")
                .departureTime(OffsetDateTime.parse("2026-04-02T08:00:00Z"))
                .build();

        when(restTemplate.exchange(
                eq("https://routes.googleapis.com/directions/v2:computeRoutes"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(String.class)))
                        .thenReturn(ResponseEntity.ok("""
                                {
                                  "routes": [
                                    {
                                      "legs": [
                                        {
                                          "steps": [
                                            {
                                              "travelMode": "TRANSIT",
                                              "transitDetails": {
                                                "stopDetails": {
                                                  "departureStop": { "name": "Hamburg Hauptbahnhof" },
                                                  "arrivalStop": { "name": "Braunschweig Hauptbahnhof" }
                                                },
                                                "transitLine": {
                                                  "nameShort": "ICE 579"
                                                }
                                              }
                                            }
                                          ]
                                        }
                                      ]
                                    }
                                  ]
                                }
                                """));

        ArrayNode routes = client.computeTrainRoutes(request, false);

        assertEquals(1, routes.size());
        assertEquals("Hamburg Hauptbahnhof",
                routes.get(0).path("legs").get(0).path("steps").get(0)
                        .path("transitDetails").path("stopDetails").path("departureStop").path("name").asText());
    }

    private int countTransitSteps(JsonNode route) {
        int transitSteps = 0;

        for (JsonNode leg : route.path("legs")) {
            for (JsonNode step : leg.path("steps")) {
                if (!step.path("transitDetails").isMissingNode()) {
                    transitSteps++;
                }
            }
        }

        return transitSteps;
    }
}
