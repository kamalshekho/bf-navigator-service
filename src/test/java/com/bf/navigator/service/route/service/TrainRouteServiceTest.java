package com.bf.navigator.service.route.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bf.navigator.service.route.client.GoogleTrainRouteClient;
import com.bf.navigator.service.route.dto.TrainRouteRequestDTO;
import com.bf.navigator.service.route.dto.TrainRouteSearchResponseDTO;
import com.bf.navigator.service.station.dto.FacilityDTO;
import com.bf.navigator.service.station.dto.FacilityState;
import com.bf.navigator.service.station.dto.FacilityType;
import com.bf.navigator.service.station.dto.StationDTO;
import com.bf.navigator.service.station.service.StationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

@ExtendWith(MockitoExtension.class)
class TrainRouteServiceTest {

    @Mock
    private GoogleTrainRouteClient googleTrainRouteClient;

    @Mock
    private StationService stationService;

    @InjectMocks
    private TrainRouteService trainRouteService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void searchTrainRoutesReturnsTripsListWithAccessibilitySummary() throws Exception {
        TrainRouteRequestDTO request = TrainRouteRequestDTO.builder()
                .origin("Hamburg Hbf")
                .destination("Braunschweig Hbf")
                .departureTime(OffsetDateTime.parse("2026-04-02T08:00:00Z"))
                .build();

        ArrayNode routes = (ArrayNode) objectMapper.readTree("""
                [
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
                                "nameShort": "ICE 579",
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
                                "arrivalStop": { "name": "Braunschweig Hauptbahnhof" },
                                "departureTime": "2026-04-02T08:46:00Z",
                                "arrivalTime": "2026-04-02T11:21:00Z"
                              },
                              "transitLine": {
                                "nameShort": "ICE 781",
                                "vehicle": { "name": { "text": "Hochgeschwindigkeitszug" } },
                                "agencies": [{ "name": "DB Fernverkehr AG" }]
                              }
                            }
                          }
                        ]
                      }
                    ]
                  }
                ]
                """);

        when(googleTrainRouteClient.computeTrainRoutes(request, true)).thenReturn(routes);
        when(stationService.searchStations("Hamburg Hauptbahnhof,Hamburg Hbf")).thenReturn(List.of(
                new StationDTO("Hamburg Hbf", 12345L, 8002549L, "Hamburg", 1, "yes", "yes", true)));
        when(stationService.searchStations("Braunschweig Hauptbahnhof,Braunschweig Hbf")).thenReturn(List.of(
                new StationDTO("Braunschweig Hbf", 23456L, 8000049L, "Braunschweig", 2, "no", "yes", true)));
        when(stationService.getStationFacilities(12345L)).thenReturn(List.of(
                FacilityDTO.builder()
                        .equipmentnumber(1001L)
                        .stationnumber(12345L)
                        .type(FacilityType.ELEVATOR)
                        .state(FacilityState.ACTIVE)
                        .description("Lift to platform 7")
                        .operatorname("DB InfraGO")
                        .build()));
        when(stationService.getStationFacilities(23456L)).thenReturn(List.of(
                FacilityDTO.builder()
                        .equipmentnumber(2002L)
                        .stationnumber(23456L)
                        .type(FacilityType.ESCALATOR)
                        .state(FacilityState.INACTIVE)
                        .description("Escalator to main hall")
                        .operatorname("DB InfraGO")
                        .build()));

        TrainRouteSearchResponseDTO response = trainRouteService.searchTrainRoutes(request, true);

        assertNotNull(response);
        assertEquals(2, response.getTrips().size());
        assertEquals("LIMITED", response.getTrips().getFirst().getAccessibilitySummary().getStatus());
        assertEquals(2, response.getTrips().getFirst().getAccessibilitySummary().getTotalStations());
        assertEquals(1, response.getTrips().getFirst().getAccessibilitySummary().getStepFreeStations());
        assertEquals(1, response.getTrips().getFirst().getAccessibilitySummary().getActiveElevators());
        assertEquals(1, response.getTrips().getFirst().getAccessibilitySummary().getInactiveEscalators());
        assertEquals("ICE 781", response.getTrips().get(1).getTransits().getFirst().getTrainName());
    }

    @Test
    void searchTrainRoutesBuildsOrderedTouchpointsWithStationLevelAccessibilityDetails() throws Exception {
        TrainRouteRequestDTO request = TrainRouteRequestDTO.builder()
                .origin("Hamburg Hbf")
                .destination("Braunschweig Hbf")
                .departureTime(OffsetDateTime.parse("2026-04-02T08:00:00Z"))
                .build();

        ArrayNode routes = (ArrayNode) objectMapper.readTree("""
                [
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
                                "departureTime": "2026-04-02T08:29:00Z",
                                "arrivalTime": "2026-04-02T09:48:00Z"
                              },
                              "transitLine": {
                                "nameShort": "ICE 579",
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
                                "departureTime": "2026-04-02T10:05:00Z",
                                "arrivalTime": "2026-04-02T10:45:00Z"
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
                  }
                ]
                """);

        when(googleTrainRouteClient.computeTrainRoutes(request, true)).thenReturn(routes);
        when(stationService.searchStations("Hamburg Hauptbahnhof,Hamburg Hbf")).thenReturn(List.of(
                new StationDTO("Hamburg Hbf", 12345L, 8002549L, "Hamburg", 1, "yes", "yes", true)));
        when(stationService.searchStations("Hannover Hauptbahnhof,Hannover Hbf")).thenReturn(List.of(
                new StationDTO("Hannover Hbf", 23456L, 8000152L, "Hannover", 1, "yes", "yes", true)));
        when(stationService.searchStations("Braunschweig Hauptbahnhof,Braunschweig Hbf")).thenReturn(List.of(
                new StationDTO("Braunschweig Hbf", 34567L, 8000049L, "Braunschweig", 2, "no", "yes", true)));
        when(stationService.getStationFacilities(12345L)).thenReturn(List.of(
                FacilityDTO.builder()
                        .equipmentnumber(1001L)
                        .stationnumber(12345L)
                        .type(FacilityType.ELEVATOR)
                        .state(FacilityState.ACTIVE)
                        .description("Lift to platform 7")
                        .operatorname("DB InfraGO")
                        .build()));
        when(stationService.getStationFacilities(23456L)).thenReturn(List.of(
                FacilityDTO.builder()
                        .equipmentnumber(2002L)
                        .stationnumber(23456L)
                        .type(FacilityType.ELEVATOR)
                        .state(FacilityState.INACTIVE)
                        .description("Lift to platform 12")
                        .operatorname("DB InfraGO")
                        .build(),
                FacilityDTO.builder()
                        .equipmentnumber(2003L)
                        .stationnumber(23456L)
                        .type(FacilityType.ESCALATOR)
                        .state(FacilityState.ACTIVE)
                        .description("Escalator to the concourse")
                        .operatorname("DB InfraGO")
                        .build()));
        when(stationService.getStationFacilities(34567L)).thenThrow(new RuntimeException("Facilities unavailable"));

        TrainRouteSearchResponseDTO response = trainRouteService.searchTrainRoutes(request, true);

        assertNotNull(response);
        assertEquals(1, response.getTrips().size());
        assertEquals(3, response.getTrips().getFirst().getTouchpoints().size());

        assertEquals("ORIGIN", response.getTrips().getFirst().getTouchpoints().get(0).getKind());
        assertEquals("TRANSFER", response.getTrips().getFirst().getTouchpoints().get(1).getKind());
        assertEquals("DESTINATION", response.getTrips().getFirst().getTouchpoints().get(2).getKind());

        assertEquals("ACCESSIBLE",
                response.getTrips().getFirst().getTouchpoints().get(0).getAccessibility().getStatus());
        assertEquals("LIMITED",
                response.getTrips().getFirst().getTouchpoints().get(1).getAccessibility().getStatus());
        assertEquals(1,
                response.getTrips().getFirst().getTouchpoints().get(1).getAccessibility().getInactiveElevators());
        assertEquals(1,
                response.getTrips().getFirst().getTouchpoints().get(1).getAccessibility().getActiveEscalators());
        assertEquals("2026-04-02T09:48:00Z", response.getTrips().getFirst().getTouchpoints().get(1).getArrivalTime());
        assertEquals("2026-04-02T10:05:00Z",
                response.getTrips().getFirst().getTouchpoints().get(1).getDepartureTime());

        assertEquals("LIMITED",
                response.getTrips().getFirst().getTouchpoints().get(2).getAccessibility().getStatus());
        assertNull(response.getTrips().getFirst().getTouchpoints().get(2).getFacilities());
        assertEquals(false,
                response.getTrips().getFirst().getTouchpoints().get(2).getAccessibility().getHasFacilityData());
    }

    @Test
    void searchTrainRoutesMarksTouchpointAccessibilityUnknownWhenStationLookupReturnsNoResult() throws Exception {
        TrainRouteRequestDTO request = TrainRouteRequestDTO.builder()
                .origin("Hamburg Hbf")
                .destination("Mystery Hbf")
                .departureTime(OffsetDateTime.parse("2026-04-02T08:00:00Z"))
                .build();

        ArrayNode routes = (ArrayNode) objectMapper.readTree("""
                [
                  {
                    "localizedValues": {
                      "distance": { "text": "120 km" },
                      "duration": { "text": "1 Stunde, 20 Minuten" }
                    },
                    "legs": [
                      {
                        "steps": [
                          {
                            "travelMode": "TRANSIT",
                            "transitDetails": {
                              "stopDetails": {
                                "departureStop": { "name": "Hamburg Hauptbahnhof" },
                                "arrivalStop": { "name": "Mystery Hauptbahnhof" },
                                "departureTime": "2026-04-02T08:29:00Z",
                                "arrivalTime": "2026-04-02T09:49:00Z"
                              },
                              "transitLine": {
                                "nameShort": "ICE 999",
                                "vehicle": { "name": { "text": "Hochgeschwindigkeitszug" } },
                                "agencies": [{ "name": "DB Fernverkehr AG" }]
                              }
                            }
                          }
                        ]
                      }
                    ]
                  }
                ]
                """);

        when(googleTrainRouteClient.computeTrainRoutes(request, true)).thenReturn(routes);
        when(stationService.searchStations("Hamburg Hauptbahnhof,Hamburg Hbf")).thenReturn(List.of(
                new StationDTO("Hamburg Hbf", 12345L, 8002549L, "Hamburg", 1, "yes", "yes", true)));
        when(stationService.searchStations("Mystery Hauptbahnhof,Mystery Hbf")).thenReturn(List.of());
        when(stationService.getStationFacilities(12345L)).thenReturn(List.of());

        TrainRouteSearchResponseDTO response = trainRouteService.searchTrainRoutes(request, true);

        assertEquals("UNKNOWN",
                response.getTrips().getFirst().getTouchpoints().get(1).getAccessibility().getStatus());
        assertNull(response.getTrips().getFirst().getTouchpoints().get(1).getStation());
        assertNull(response.getTrips().getFirst().getTouchpoints().get(1).getFacilities());
        assertEquals(false,
                response.getTrips().getFirst().getTouchpoints().get(1).getAccessibility().getHasFacilityData());
    }

    @Test
    void searchTrainRoutesAddsDepartureAndArrivalStopCoordinatesToTouchpoints() throws Exception {
        TrainRouteRequestDTO request = TrainRouteRequestDTO.builder()
                .origin("Hamburg Hbf")
                .destination("Braunschweig Hbf")
                .departureTime(OffsetDateTime.parse("2026-04-02T08:00:00Z"))
                .build();

        ArrayNode routes = (ArrayNode) objectMapper.readTree("""
                [
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
                                "departureStop": {
                                  "name": "Hamburg Hauptbahnhof",
                                  "location": {
                                    "latLng": { "latitude": 53.552776, "longitude": 10.006603 }
                                  }
                                },
                                "arrivalStop": {
                                  "name": "Braunschweig Hauptbahnhof",
                                  "location": {
                                    "latLng": { "latitude": 52.253164, "longitude": 10.54058 }
                                  }
                                },
                                "departureTime": "2026-04-02T08:29:00Z",
                                "arrivalTime": "2026-04-02T10:45:00Z"
                              },
                              "transitLine": {
                                "nameShort": "ICE 579",
                                "vehicle": { "name": { "text": "Hochgeschwindigkeitszug" } },
                                "agencies": [{ "name": "DB Fernverkehr AG" }]
                              }
                            }
                          }
                        ]
                      }
                    ]
                  }
                ]
                """);

        when(googleTrainRouteClient.computeTrainRoutes(request, true)).thenReturn(routes);
        when(stationService.searchStations("Hamburg Hauptbahnhof,Hamburg Hbf")).thenReturn(List.of());
        when(stationService.searchStations("Braunschweig Hauptbahnhof,Braunschweig Hbf")).thenReturn(List.of());

        TrainRouteSearchResponseDTO response = trainRouteService.searchTrainRoutes(request, true);

        assertEquals(53.552776,
                response.getTrips().getFirst().getTouchpoints().get(0).getDepartureStop().getLatitude(),
                0.000001);
        assertEquals(10.006603,
                response.getTrips().getFirst().getTouchpoints().get(0).getDepartureStop().getLongitude(),
                0.000001);
        assertEquals(52.253164,
                response.getTrips().getFirst().getTouchpoints().get(1).getArrivalStop().getLatitude(),
                0.000001);
        assertEquals(10.54058,
                response.getTrips().getFirst().getTouchpoints().get(1).getArrivalStop().getLongitude(),
                0.000001);
    }

    @Test
    void searchTrainRoutesBuildsWalkingApproachFromLastWalkStepBeforeOriginTransit() throws Exception {
        TrainRouteRequestDTO request = TrainRouteRequestDTO.builder()
                .origin("Hamburg Hbf")
                .destination("Braunschweig Hbf")
                .departureTime(OffsetDateTime.parse("2026-04-02T08:00:00Z"))
                .build();

        ArrayNode routes = (ArrayNode) objectMapper.readTree("""
                [
                  {
                    "localizedValues": {
                      "distance": { "text": "240 km" },
                      "duration": { "text": "2 Stunden, 20 Minuten" }
                    },
                    "legs": [
                      {
                        "steps": [
                          {
                            "travelMode": "WALK",
                            "endLocation": {
                              "latLng": { "latitude": 53.552321, "longitude": 10.00611 }
                            }
                          },
                          {
                            "travelMode": "WALK",
                            "endLocation": {
                              "latLng": { "latitude": 53.552776, "longitude": 10.006603 }
                            },
                            "navigationInstruction": { "instructions": "Hier einsteigen: E" }
                          },
                          {
                            "travelMode": "TRANSIT",
                            "transitDetails": {
                              "stopDetails": {
                                "departureStop": {
                                  "name": "Hamburg Hauptbahnhof",
                                  "location": {
                                    "latLng": { "latitude": 53.552776, "longitude": 10.006603 }
                                  }
                                },
                                "arrivalStop": {
                                  "name": "Braunschweig Hauptbahnhof",
                                  "location": {
                                    "latLng": { "latitude": 52.253164, "longitude": 10.54058 }
                                  }
                                },
                                "departureTime": "2026-04-02T08:29:00Z",
                                "arrivalTime": "2026-04-02T10:45:00Z"
                              },
                              "transitLine": {
                                "nameShort": "ICE 579",
                                "vehicle": { "name": { "text": "Hochgeschwindigkeitszug" } },
                                "agencies": [{ "name": "DB Fernverkehr AG" }]
                              }
                            }
                          }
                        ]
                      }
                    ]
                  }
                ]
                """);

        when(googleTrainRouteClient.computeTrainRoutes(request, true)).thenReturn(routes);
        when(stationService.searchStations("Hamburg Hauptbahnhof,Hamburg Hbf")).thenReturn(List.of());
        when(stationService.searchStations("Braunschweig Hauptbahnhof,Braunschweig Hbf")).thenReturn(List.of());

        TrainRouteSearchResponseDTO response = trainRouteService.searchTrainRoutes(request, true);

        assertNotNull(response.getTrips().getFirst().getTouchpoints().get(0).getWalkingApproach());
        assertEquals("Hier einsteigen: E",
                response.getTrips().getFirst().getTouchpoints().get(0).getWalkingApproach().getInstruction());
        assertEquals(53.552776,
                response.getTrips().getFirst().getTouchpoints().get(0).getWalkingApproach().getLatitude(),
                0.000001);
        assertEquals(10.006603,
                response.getTrips().getFirst().getTouchpoints().get(0).getWalkingApproach().getLongitude(),
                0.000001);
    }

    @Test
    void searchTrainRoutesBuildsWalkingApproachForTransferDepartureWhenWalkPrecedesNextTransit() throws Exception {
        TrainRouteRequestDTO request = TrainRouteRequestDTO.builder()
                .origin("Hamburg Hbf")
                .destination("Braunschweig Hbf")
                .departureTime(OffsetDateTime.parse("2026-04-02T08:00:00Z"))
                .build();

        ArrayNode routes = (ArrayNode) objectMapper.readTree("""
                [
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
                                "departureTime": "2026-04-02T08:29:00Z",
                                "arrivalTime": "2026-04-02T09:48:00Z"
                              },
                              "transitLine": {
                                "nameShort": "ICE 579",
                                "vehicle": { "name": { "text": "Hochgeschwindigkeitszug" } },
                                "agencies": [{ "name": "DB Fernverkehr AG" }]
                              }
                            }
                          },
                          {
                            "travelMode": "WALK",
                            "endLocation": {
                              "latLng": { "latitude": 52.3772, "longitude": 9.7416 }
                            },
                            "navigationInstruction": { "instructions": "Zum Gleis 8 wechseln" }
                          },
                          {
                            "travelMode": "TRANSIT",
                            "transitDetails": {
                              "stopDetails": {
                                "departureStop": { "name": "Hannover Hauptbahnhof" },
                                "arrivalStop": { "name": "Braunschweig Hauptbahnhof" },
                                "departureTime": "2026-04-02T10:05:00Z",
                                "arrivalTime": "2026-04-02T10:45:00Z"
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
                  }
                ]
                """);

        when(googleTrainRouteClient.computeTrainRoutes(request, true)).thenReturn(routes);
        when(stationService.searchStations("Hamburg Hauptbahnhof,Hamburg Hbf")).thenReturn(List.of());
        when(stationService.searchStations("Hannover Hauptbahnhof,Hannover Hbf")).thenReturn(List.of());
        when(stationService.searchStations("Braunschweig Hauptbahnhof,Braunschweig Hbf")).thenReturn(List.of());

        TrainRouteSearchResponseDTO response = trainRouteService.searchTrainRoutes(request, true);

        assertEquals(3, response.getTrips().getFirst().getTouchpoints().size());
        assertEquals("TRANSFER", response.getTrips().getFirst().getTouchpoints().get(1).getKind());
        assertNotNull(response.getTrips().getFirst().getTouchpoints().get(1).getWalkingApproach());
        assertEquals("Zum Gleis 8 wechseln",
                response.getTrips().getFirst().getTouchpoints().get(1).getWalkingApproach().getInstruction());
        assertEquals(52.3772,
                response.getTrips().getFirst().getTouchpoints().get(1).getWalkingApproach().getLatitude(),
                0.000001);
        assertEquals(9.7416,
                response.getTrips().getFirst().getTouchpoints().get(1).getWalkingApproach().getLongitude(),
                0.000001);
    }

    @Test
    void searchTrainRoutesKeepsLastValidWalkingApproachWhenFollowingWalkIsInvalid() throws Exception {
        TrainRouteRequestDTO request = TrainRouteRequestDTO.builder()
                .origin("Hamburg Hbf")
                .destination("Braunschweig Hbf")
                .departureTime(OffsetDateTime.parse("2026-04-02T08:00:00Z"))
                .build();

        ArrayNode routes = (ArrayNode) objectMapper.readTree("""
                [
                  {
                    "legs": [
                      {
                        "steps": [
                          {
                            "travelMode": "WALK",
                            "endLocation": {
                              "latLng": { "latitude": 53.552776, "longitude": 10.006603 }
                            },
                            "navigationInstruction": { "instructions": "Zum Gleis 7 gehen" }
                          },
                          {
                            "travelMode": "WALK",
                            "navigationInstruction": { "instructions": "Ungültiger Walk ohne Koordinaten" }
                          },
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
                                "nameShort": "ICE 579",
                                "vehicle": { "name": { "text": "Hochgeschwindigkeitszug" } },
                                "agencies": [{ "name": "DB Fernverkehr AG" }]
                              }
                            }
                          }
                        ]
                      }
                    ]
                  }
                ]
                """);

        when(googleTrainRouteClient.computeTrainRoutes(request, true)).thenReturn(routes);
        when(stationService.searchStations("Hamburg Hauptbahnhof,Hamburg Hbf")).thenReturn(List.of());
        when(stationService.searchStations("Braunschweig Hauptbahnhof,Braunschweig Hbf")).thenReturn(List.of());

        TrainRouteSearchResponseDTO response = trainRouteService.searchTrainRoutes(request, true);

        assertNotNull(response.getTrips().getFirst().getTouchpoints().get(0).getWalkingApproach());
        assertEquals("Zum Gleis 7 gehen",
                response.getTrips().getFirst().getTouchpoints().get(0).getWalkingApproach().getInstruction());
        assertEquals(53.552776,
                response.getTrips().getFirst().getTouchpoints().get(0).getWalkingApproach().getLatitude(),
                0.000001);
        assertEquals(10.006603,
                response.getTrips().getFirst().getTouchpoints().get(0).getWalkingApproach().getLongitude(),
                0.000001);
    }

    @Test
    void searchTrainRoutesEscapesMarkupInWalkingApproachInstruction() throws Exception {
        TrainRouteRequestDTO request = TrainRouteRequestDTO.builder()
                .origin("Hamburg Hbf")
                .destination("Braunschweig Hbf")
                .departureTime(OffsetDateTime.parse("2026-04-02T08:00:00Z"))
                .build();

        ArrayNode routes = (ArrayNode) objectMapper.readTree("""
                [
                  {
                    "legs": [
                      {
                        "steps": [
                          {
                            "travelMode": "WALK",
                            "endLocation": {
                              "latLng": { "latitude": 53.552776, "longitude": 10.006603 }
                            },
                            "navigationInstruction": { "instructions": "<b>Zum Gleis 7 gehen</b>" }
                          },
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
                                "nameShort": "ICE 579",
                                "vehicle": { "name": { "text": "Hochgeschwindigkeitszug" } },
                                "agencies": [{ "name": "DB Fernverkehr AG" }]
                              }
                            }
                          }
                        ]
                      }
                    ]
                  }
                ]
                """);

        when(googleTrainRouteClient.computeTrainRoutes(request, true)).thenReturn(routes);
        when(stationService.searchStations("Hamburg Hauptbahnhof,Hamburg Hbf")).thenReturn(List.of());
        when(stationService.searchStations("Braunschweig Hauptbahnhof,Braunschweig Hbf")).thenReturn(List.of());

        TrainRouteSearchResponseDTO response = trainRouteService.searchTrainRoutes(request, true);

        assertEquals("&lt;b&gt;Zum Gleis 7 gehen&lt;/b&gt;",
                response.getTrips().getFirst().getTouchpoints().get(0).getWalkingApproach().getInstruction());
    }

    @Test
    void searchTrainRoutesSkipsWalkingApproachWhenLatLngAreNonNumeric() throws Exception {
        TrainRouteRequestDTO request = TrainRouteRequestDTO.builder()
                .origin("Hamburg Hbf")
                .destination("Braunschweig Hbf")
                .departureTime(OffsetDateTime.parse("2026-04-02T08:00:00Z"))
                .build();

        ArrayNode routes = (ArrayNode) objectMapper.readTree("""
                [
                  {
                    "legs": [
                      {
                        "steps": [
                          {
                            "travelMode": "WALK",
                            "endLocation": {
                              "latLng": { "latitude": "north", "longitude": "east" }
                            },
                            "navigationInstruction": { "instructions": "Zum Gleis 7 gehen" }
                          },
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
                                "nameShort": "ICE 579",
                                "vehicle": { "name": { "text": "Hochgeschwindigkeitszug" } },
                                "agencies": [{ "name": "DB Fernverkehr AG" }]
                              }
                            }
                          }
                        ]
                      }
                    ]
                  }
                ]
                """);

        when(googleTrainRouteClient.computeTrainRoutes(request, true)).thenReturn(routes);
        when(stationService.searchStations("Hamburg Hauptbahnhof,Hamburg Hbf")).thenReturn(List.of());
        when(stationService.searchStations("Braunschweig Hauptbahnhof,Braunschweig Hbf")).thenReturn(List.of());

        TrainRouteSearchResponseDTO response = trainRouteService.searchTrainRoutes(request, true);

        assertNull(response.getTrips().getFirst().getTouchpoints().get(0).getWalkingApproach());
    }

    @Test
    void searchTrainRoutesKeepsWalkingApproachWhenInvalidTransitIsFollowedByValidTransit() throws Exception {
        TrainRouteRequestDTO request = TrainRouteRequestDTO.builder()
                .origin("Hamburg Hbf")
                .destination("Braunschweig Hbf")
                .departureTime(OffsetDateTime.parse("2026-04-02T08:00:00Z"))
                .build();

        ArrayNode routes = (ArrayNode) objectMapper.readTree("""
                [
                  {
                    "legs": [
                      {
                        "steps": [
                          {
                            "travelMode": "WALK",
                            "endLocation": {
                              "latLng": { "latitude": 53.552776, "longitude": 10.006603 }
                            },
                            "navigationInstruction": { "instructions": "Zum Gleis 7 gehen" }
                          },
                          {
                            "travelMode": "TRANSIT",
                            "transitDetails": {
                              "stopDetails": {
                                "departureStop": { },
                                "arrivalStop": { "name": "Hannover Hauptbahnhof" },
                                "departureTime": "2026-04-02T08:29:00Z",
                                "arrivalTime": "2026-04-02T09:48:00Z"
                              },
                              "transitLine": {
                                "nameShort": "ICE 579",
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
                                "departureTime": "2026-04-02T10:05:00Z",
                                "arrivalTime": "2026-04-02T10:45:00Z"
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
                  }
                ]
                """);

        when(googleTrainRouteClient.computeTrainRoutes(request, true)).thenReturn(routes);
        when(stationService.searchStations("Hannover Hauptbahnhof,Hannover Hbf")).thenReturn(List.of());
        when(stationService.searchStations("Braunschweig Hauptbahnhof,Braunschweig Hbf")).thenReturn(List.of());

        TrainRouteSearchResponseDTO response = trainRouteService.searchTrainRoutes(request, true);

        assertEquals(1, response.getTrips().getFirst().getTransits().size());
        assertNotNull(response.getTrips().getFirst().getTouchpoints().get(0).getWalkingApproach());
        assertEquals("Zum Gleis 7 gehen",
                response.getTrips().getFirst().getTouchpoints().get(0).getWalkingApproach().getInstruction());
        assertEquals(53.552776,
                response.getTrips().getFirst().getTouchpoints().get(0).getWalkingApproach().getLatitude(),
                0.000001);
        assertEquals(10.006603,
                response.getTrips().getFirst().getTouchpoints().get(0).getWalkingApproach().getLongitude(),
                0.000001);
    }

    @Test
    void searchTrainRoutesKeepsRegionalTransitSegmentsFromFullGooglePayload() throws Exception {
        TrainRouteRequestDTO request = TrainRouteRequestDTO.builder()
                .origin("Hamburg Hbf")
                .destination("Braunschweig Hbf")
                .departureTime(OffsetDateTime.parse("2026-04-02T08:00:00Z"))
                .build();

        ArrayNode routes = (ArrayNode) objectMapper.readTree("""
                [
                  {
                    "localizedValues": {
                      "distance": { "text": "240 km" },
                      "duration": { "text": "2 Stunden, 20 Minuten" }
                    },
                    "legs": [
                      {
                        "steps": [
                          {
                            "travelMode": "WALK"
                          },
                          {
                            "travelMode": "TRANSIT",
                            "transitDetails": {
                              "stopDetails": {
                                "departureStop": { "name": "Hamburg Hauptbahnhof" },
                                "arrivalStop": { "name": "Hannover Hauptbahnhof" },
                                "departureTime": "2026-04-02T08:29:00Z",
                                "arrivalTime": "2026-04-02T09:48:00Z"
                              },
                              "transitLine": {
                                "nameShort": "ICE 579",
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
                                "departureTime": "2026-04-02T09:55:00Z",
                                "arrivalTime": "2026-04-02T10:45:00Z"
                              },
                              "transitLine": {
                                "name": "RE70",
                                "vehicle": { "name": { "text": "Zug oder S-Bahn" } },
                                "agencies": [{ "name": "Westfalenbahn" }]
                              }
                            }
                          }
                        ]
                      }
                    ]
                  }
                ]
                """);

        when(googleTrainRouteClient.computeTrainRoutes(request, false)).thenReturn(routes);
        when(stationService.searchStations("Hamburg Hauptbahnhof,Hamburg Hbf")).thenReturn(List.of(
                new StationDTO("Hamburg Hbf", 12345L, 8002549L, "Hamburg", 1, "yes", "yes", true)));
        when(stationService.searchStations("Hannover Hauptbahnhof,Hannover Hbf")).thenReturn(List.of(
                new StationDTO("Hannover Hbf", 23456L, 8000152L, "Hannover", 1, "yes", "yes", true)));
        when(stationService.searchStations("Braunschweig Hauptbahnhof,Braunschweig Hbf")).thenReturn(List.of(
                new StationDTO("Braunschweig Hbf", 34567L, 8000049L, "Braunschweig", 2, "yes", "yes", true)));

        TrainRouteSearchResponseDTO response = trainRouteService.searchTrainRoutes(request, false);

        assertEquals(1, response.getTrips().size());
        assertEquals(2, response.getTrips().getFirst().getTransits().size());
        assertEquals("ICE 579", response.getTrips().getFirst().getTransits().get(0).getTrainName());
        assertEquals("RE70", response.getTrips().getFirst().getTransits().get(1).getTrainName());
        assertEquals("Westfalenbahn", response.getTrips().getFirst().getTransits().get(1).getAgencyName());
    }

    @Test
    void resolveTrainNameFallsBackToTransitLineNameWhenNameShortIsMissing() throws Exception {
        TrainRouteRequestDTO request = TrainRouteRequestDTO.builder()
                .origin("Hannover Hbf")
                .destination("Braunschweig Hbf")
                .departureTime(OffsetDateTime.parse("2026-04-02T09:00:00Z"))
                .build();

        ArrayNode routes = (ArrayNode) objectMapper.readTree("""
                [
                  {
                    "legs": [
                      {
                        "steps": [
                          {
                            "travelMode": "TRANSIT",
                            "transitDetails": {
                              "stopDetails": {
                                "departureStop": { "name": "Hannover Hauptbahnhof" },
                                "arrivalStop": { "name": "Braunschweig Hauptbahnhof" },
                                "departureTime": "2026-04-02T09:55:00Z",
                                "arrivalTime": "2026-04-02T10:45:00Z"
                              },
                              "transitLine": {
                                "name": "RE70",
                                "vehicle": { "name": { "text": "Zug oder S-Bahn" } },
                                "agencies": [{ "name": "Westfalenbahn" }]
                              }
                            }
                          }
                        ]
                      }
                    ]
                  }
                ]
                """);

        when(googleTrainRouteClient.computeTrainRoutes(request, false)).thenReturn(routes);
        when(stationService.searchStations("Hannover Hauptbahnhof,Hannover Hbf")).thenReturn(List.of(
                new StationDTO("Hannover Hbf", 23456L, 8000152L, "Hannover", 1, "yes", "yes", true)));
        when(stationService.searchStations("Braunschweig Hauptbahnhof,Braunschweig Hbf")).thenReturn(List.of(
                new StationDTO("Braunschweig Hbf", 34567L, 8000049L, "Braunschweig", 2, "yes", "yes", true)));

        TrainRouteSearchResponseDTO response = trainRouteService.searchTrainRoutes(request, false);

        assertEquals("RE70", response.getTrips().getFirst().getTransits().getFirst().getTrainName());
    }

    @Test
    void searchTrainRoutesKeepsTransitWhenStationEnrichmentFails() throws Exception {
        TrainRouteRequestDTO request = TrainRouteRequestDTO.builder()
                .origin("Hannover Hbf")
                .destination("Braunschweig Hbf")
                .departureTime(OffsetDateTime.parse("2026-04-02T09:00:00Z"))
                .build();

        ArrayNode routes = (ArrayNode) objectMapper.readTree("""
                [
                  {
                    "legs": [
                      {
                        "steps": [
                          {
                            "travelMode": "TRANSIT",
                            "transitDetails": {
                              "stopDetails": {
                                "departureStop": { "name": "Hannover Hauptbahnhof" },
                                "arrivalStop": { "name": "Braunschweig Hauptbahnhof" },
                                "departureTime": "2026-04-02T09:55:00Z",
                                "arrivalTime": "2026-04-02T10:45:00Z"
                              },
                              "transitLine": {
                                "name": "RE70",
                                "vehicle": { "name": { "text": "Zug oder S-Bahn" } },
                                "agencies": [{ "name": "Westfalenbahn" }]
                              }
                            }
                          }
                        ]
                      }
                    ]
                  }
                ]
                """);

        when(googleTrainRouteClient.computeTrainRoutes(request, false)).thenReturn(routes);
        when(stationService.searchStations("Hannover Hauptbahnhof,Hannover Hbf"))
                .thenThrow(new RuntimeException("station lookup failed"));
        when(stationService.searchStations("Braunschweig Hauptbahnhof,Braunschweig Hbf"))
                .thenThrow(new RuntimeException("station lookup failed"));

        TrainRouteSearchResponseDTO response = trainRouteService.searchTrainRoutes(request, false);

        assertEquals(1, response.getTrips().size());
        assertEquals(1, response.getTrips().getFirst().getTransits().size());
        assertEquals("RE70", response.getTrips().getFirst().getTransits().getFirst().getTrainName());
    }
}
