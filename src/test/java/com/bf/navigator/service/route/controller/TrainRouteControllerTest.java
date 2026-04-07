package com.bf.navigator.service.route.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.OffsetDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.bf.navigator.service.route.dto.TrainRouteAccessibilitySummaryDTO;
import com.bf.navigator.service.route.dto.TrainRouteRequestDTO;
import com.bf.navigator.service.route.dto.TrainRouteResponseDTO;
import com.bf.navigator.service.route.dto.TrainRouteSearchResponseDTO;
import com.bf.navigator.service.route.service.TrainRouteService;

@ExtendWith(MockitoExtension.class)
class TrainRouteControllerTest {

    @Mock
    private TrainRouteService trainRouteService;

    @InjectMocks
    private TrainRouteController trainRouteController;

    @Test
    void getTrainRouteReturnsStableSearchContract() {
        TrainRouteRequestDTO request = TrainRouteRequestDTO.builder()
                .origin("Hamburg Hbf")
                .destination("Braunschweig Hbf")
                .departureTime(OffsetDateTime.parse("2026-04-02T08:00:00Z"))
                .build();
        TrainRouteResponseDTO trip = TrainRouteResponseDTO.builder()
                .origin("Hamburg Hauptbahnhof")
                .destination("Braunschweig Hauptbahnhof")
                .departureTime("2026-04-02T08:29:00Z")
                .arrivalTime("2026-04-02T10:45:00Z")
                .localizedDistanceText("240 km")
                .localizedDurationText("2 Stunden, 20 Minuten")
                .accessibilitySummary(TrainRouteAccessibilitySummaryDTO.builder()
                        .status("ACCESSIBLE")
                        .summary("2/2 stations step-free")
                        .totalStations(2)
                        .stepFreeStations(2)
                        .mobilityServiceStations(2)
                        .activeElevators(2)
                        .inactiveElevators(0)
                        .activeEscalators(1)
                        .inactiveEscalators(0)
                        .build())
                .transits(List.of())
                .build();
        TrainRouteSearchResponseDTO response = TrainRouteSearchResponseDTO.builder()
                .trips(List.of(trip))
                .build();

        when(trainRouteService.searchTrainRoutes(request, false)).thenReturn(response);

        ResponseEntity<TrainRouteSearchResponseDTO> result = trainRouteController.getTrainRoute(request);

        assertTrue(result.getStatusCode().is2xxSuccessful());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().getTrips().size());
        assertEquals("ACCESSIBLE", result.getBody().getTrips().getFirst().getAccessibilitySummary().getStatus());
        verify(trainRouteService).searchTrainRoutes(request, false);
    }

    @Test
    void getDebugTrainRouteReturnsStableSearchContract() {
        TrainRouteRequestDTO request = TrainRouteRequestDTO.builder()
                .origin("Hamburg Hbf")
                .destination("Braunschweig Hbf")
                .departureTime(OffsetDateTime.parse("2026-04-02T08:00:00Z"))
                .build();
        TrainRouteResponseDTO trip = TrainRouteResponseDTO.builder()
                .origin("Hamburg Hauptbahnhof")
                .destination("Braunschweig Hauptbahnhof")
                .departureTime("2026-04-02T08:29:00Z")
                .arrivalTime("2026-04-02T10:45:00Z")
                .localizedDistanceText("240 km")
                .localizedDurationText("2 Stunden, 20 Minuten")
                .accessibilitySummary(TrainRouteAccessibilitySummaryDTO.builder()
                        .status("ACCESSIBLE")
                        .summary("2/2 stations step-free")
                        .totalStations(2)
                        .stepFreeStations(2)
                        .mobilityServiceStations(2)
                        .activeElevators(2)
                        .inactiveElevators(0)
                        .activeEscalators(1)
                        .inactiveEscalators(0)
                        .build())
                .transits(List.of())
                .build();
        TrainRouteSearchResponseDTO response = TrainRouteSearchResponseDTO.builder()
                .trips(List.of(trip))
                .build();

        when(trainRouteService.searchTrainRoutes(request, true)).thenReturn(response);

        ResponseEntity<TrainRouteSearchResponseDTO> result = trainRouteController.getDebugTrainRoute(request);

        assertTrue(result.getStatusCode().is2xxSuccessful());
        assertNotNull(result.getBody());
        assertEquals(1, result.getBody().getTrips().size());
        assertEquals("ACCESSIBLE", result.getBody().getTrips().getFirst().getAccessibilitySummary().getStatus());
        verify(trainRouteService).searchTrainRoutes(request, true);
    }
}
