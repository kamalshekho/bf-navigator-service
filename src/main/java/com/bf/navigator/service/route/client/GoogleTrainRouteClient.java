package com.bf.navigator.service.route.client;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.bf.navigator.service.route.dto.TrainRouteRequestDTO;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class GoogleTrainRouteClient {

    private static final String BASE_URL = "https://routes.googleapis.com/directions/v2:computeRoutes";
    private static final String FULL_RESPONSE_FIELD_MASK = "*";

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final String apiKey;

    public GoogleTrainRouteClient(RestTemplate restTemplate,
            ObjectMapper objectMapper,
            @Value("${google.routes.api-key:}") String apiKey) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
        this.apiKey = apiKey;
    }

    public ArrayNode computeTrainRoutes(TrainRouteRequestDTO request, boolean debug) {
        try {
            String rawResponse = debug
                    ? computeTrainRoutesRaw(request, FULL_RESPONSE_FIELD_MASK, true)
                    : computeTrainRoutesAllData(request);

            JsonNode rootNode = objectMapper.readTree(rawResponse);
            JsonNode routesNode = rootNode.path("routes");
            return routesNode.isArray() ? (ArrayNode) routesNode : objectMapper.createArrayNode();
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse Google Routes response", e);
        }
    }

    public String computeTrainRoutesRaw(TrainRouteRequestDTO request, String field_mask, boolean debug) {

        if (debug) {
            return DEBUG_GOOGLE_MAPS_RESPONSE;
        }

        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalStateException("Missing Google Routes API key. Configure google.routes.api-key");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.APPLICATION_JSON));
        headers.set("X-Goog-Api-Key", apiKey);
        headers.set("X-Goog-FieldMask", field_mask);

        try {
            String requestBody = objectMapper.writeValueAsString(buildRequestBody(request));
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
            ResponseEntity<String> response = restTemplate.exchange(BASE_URL, HttpMethod.POST, entity, String.class);
            System.out.println("Google API endpoint was called");
            return response.getBody();
        } catch (Exception e) {
            throw new RuntimeException("Failed to call Google Routes API", e);
        }
    }

    public String computeTrainRoutesAllData(TrainRouteRequestDTO request) {
        return computeTrainRoutesRaw(request, "*", false);
    }

    private ObjectNode buildRequestBody(TrainRouteRequestDTO request) {
        ObjectNode body = objectMapper.createObjectNode();
        body.set("origin", waypoint(request.getOrigin()));
        body.set("destination", waypoint(request.getDestination()));
        body.put("travelMode", "TRANSIT");
        body.put("departureTime", request.getDepartureTime().toInstant().toString());
        body.put("computeAlternativeRoutes", true);

        ObjectNode transitPreferences = objectMapper.createObjectNode();
        ArrayNode allowedTravelModes = objectMapper.createArrayNode();
        allowedTravelModes.add("TRAIN");
        transitPreferences.set("allowedTravelModes", allowedTravelModes);
        body.set("transitPreferences", transitPreferences);
        return body;
    }

    private ObjectNode waypoint(String address) {
        ObjectNode waypoint = objectMapper.createObjectNode();
        waypoint.put("address", address);
        return waypoint;
    }

    // Compact debug response because we have limited free Google Routes quota.
    private static final String DEBUG_GOOGLE_MAPS_RESPONSE = """
                {
                  "routes": [
                    {
                      "localizedValues": {
                        "distance": { "text": "240 km" },
                        "duration": { "text": "2 Stunden, 20 Minuten" }
                      },
                      "legs": [
                        {
                          "steps": [
                            {
                              "travelMode": "TRANSIT",
                              "transitDetails": {
                                "stopDetails": {
                                  "departureStop": { "name": "Hamburg Hauptbahnhof" },
                                  "arrivalStop": { "name": "Braunschweig Hauptbahnhof" },
                                  "departureTime": "2026-04-02T08:29:00Z",
                                  "arrivalTime": "2026-04-02T10:45:00Z"
                                },
                                "transitLine": {
                                  "nameShort": "ICE 73",
                                  "vehicle": { "name": { "text": "Hochgeschwindigkeitszug" } },
                                  "agencies": [{ "name": "DB Fernverkehr AG" }]
                                }
                              }
                            }
                          ]
                        }
                      ]
                    },
                    {
                      "localizedValues": {
                        "distance": { "text": "248 km" },
                        "duration": { "text": "2 Stunden, 35 Minuten" }
                      },
                      "legs": [
                        {
                          "steps": [
                            {
                              "travelMode": "TRANSIT",
                              "transitDetails": {
                                "stopDetails": {
                                  "departureStop": { "name": "Hamburg Hauptbahnhof" },
                                  "arrivalStop": { "name": "Hannover Hauptbahnhof" },
                                  "departureTime": "2026-04-02T08:41:00Z",
                                  "arrivalTime": "2026-04-02T09:58:00Z"
                                },
                                "transitLine": {
                                  "nameShort": "ICE 585",
                                  "vehicle": { "name": { "text": "Hochgeschwindigkeitszug" } },
                                  "agencies": [{ "name": "DB Fernverkehr AG" }]
                                }
                              }
                            },
                            {
                              "travelMode": "TRANSIT",
                              "transitDetails": {
                                "stopDetails": {
                                  "departureStop": { "name": "Hannover Hauptbahnhof" },
                                  "arrivalStop": { "name": "Braunschweig Hauptbahnhof" },
                                  "departureTime": "2026-04-02T10:10:00Z",
                                  "arrivalTime": "2026-04-02T10:58:00Z"
                                },
                                "transitLine": {
                                  "nameShort": "IC 2038",
                                  "vehicle": { "name": { "text": "Intercity" } },
                                  "agencies": [{ "name": "DB Fernverkehr AG" }]
                                }
                              }
                            }
                          ]
                        }
                      ]
                    },
                    {
                      "localizedValues": {
                        "distance": { "text": "266 km" },
                        "duration": { "text": "3 Stunden, 7 Minuten" }
                      },
                      "legs": [
                        {
                          "steps": [
                            {
                              "travelMode": "TRANSIT",
                              "transitDetails": {
                                "stopDetails": {
                                  "departureStop": { "name": "Hamburg Hauptbahnhof" },
                                  "arrivalStop": { "name": "Uelzen Bahnhof" },
                                  "departureTime": "2026-04-02T08:17:00Z",
                                  "arrivalTime": "2026-04-02T09:11:00Z"
                                },
                                "transitLine": {
                                  "nameShort": "RE 3",
                                  "vehicle": { "name": { "text": "Regionalzug" } },
                                  "agencies": [{ "name": "metronom" }]
                                }
                              }
                            },
                            {
                              "travelMode": "TRANSIT",
                              "transitDetails": {
                                "stopDetails": {
                                  "departureStop": { "name": "Uelzen Bahnhof" },
                                  "arrivalStop": { "name": "Hannover Hauptbahnhof" },
                                  "departureTime": "2026-04-02T09:22:00Z",
                                  "arrivalTime": "2026-04-02T10:17:00Z"
                                },
                                "transitLine": {
                                  "nameShort": "IC 2083",
                                  "vehicle": { "name": { "text": "Intercity" } },
                                  "agencies": [{ "name": "DB Fernverkehr AG" }]
                                }
                              }
                            },
                            {
                              "travelMode": "TRANSIT",
                              "transitDetails": {
                                "stopDetails": {
                                  "departureStop": { "name": "Hannover Hauptbahnhof" },
                                  "arrivalStop": { "name": "Braunschweig Hauptbahnhof" },
                                  "departureTime": "2026-04-02T10:31:00Z",
                                  "arrivalTime": "2026-04-02T11:24:00Z"
                                },
                                "transitLine": {
                                  "nameShort": "RE 60",
                                  "vehicle": { "name": { "text": "Regionalzug" } },
                                  "agencies": [{ "name": "DB Regio" }]
                                }
                              }
                            }
                          ]
                        }
                      ]
                    }
                  ]
                }
            """;

}
