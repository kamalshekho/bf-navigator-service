package com.bf.navigator.service.route.service;

import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import com.bf.navigator.service.route.client.GoogleTrainRouteClient;
import com.bf.navigator.service.route.dto.TrainRouteRequestDTO;
import com.bf.navigator.service.route.dto.TrainRouteResponseDTO;
import com.bf.navigator.service.route.dto.TrainRouteStopDTO;
import com.bf.navigator.service.route.dto.TrainRouteTransitDTO;
import com.bf.navigator.service.station.dto.FacilityDTO;
import com.bf.navigator.service.station.dto.StationDTO;
import com.bf.navigator.service.station.service.StationService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;


@Slf4j
@Service
@RequiredArgsConstructor
public class TrainRouteService {

    private final GoogleTrainRouteClient googleTrainRouteClient;
    private final StationService stationService;


    public TrainRouteResponseDTO getTrainRoute(TrainRouteRequestDTO request, boolean debug) {
        validateRequest(request);

        ArrayNode routes = googleTrainRouteClient.computeTrainRoutes(request, debug);
        if (routes == null || routes.isEmpty()) {
            throw new RuntimeException("No train route found for the provided origin and destination");
        }

        for (JsonNode route : routes) {
            TrainRouteResponseDTO mappedRoute = mapRoute(route);
            if (mappedRoute != null) {
                return mappedRoute;
            }
        }

        throw new RuntimeException("Google Routes response did not contain train stop details");
    }


    public String getTrainRouteRaw(TrainRouteRequestDTO request) {
        validateRequest(request);
        return googleTrainRouteClient.computeTrainRoutesAllData(request);
    }


    private void validateRequest(TrainRouteRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Request must not be null");
        }
        if (request.getOrigin() == null || request.getOrigin().isBlank()) {
            throw new IllegalArgumentException("Origin must not be empty");
        }
        if (request.getDestination() == null || request.getDestination().isBlank()) {
            throw new IllegalArgumentException("Destination must not be empty");
        }
        if (request.getDepartureTime() == null) {
            throw new IllegalArgumentException("Departure time must not be null");
        }
    }


    private TrainRouteResponseDTO mapRoute(JsonNode route) {
        JsonNode legs = route.path("legs");
        if (!legs.isArray() || legs.isEmpty()) {
            return null;
        }

        List<TrainRouteTransitDTO> transits = new java.util.ArrayList<>();
        for (JsonNode leg : legs) {
            collectRouteDetails(leg.path("steps"), transits);
        }

        if (transits.isEmpty()) {
            return null;
        }

        enrichTransitsWithStationInfo(transits);

        JsonNode localizedValues = route.path("localizedValues");
        TrainRouteTransitDTO firstTransit = transits.getFirst();
        TrainRouteTransitDTO lastTransit = transits.getLast();

        return TrainRouteResponseDTO.builder()
                .origin(firstTransit.getDeparture().getStationName())
                .destination(lastTransit.getArrival().getStationName())
                .departureTime(firstTransit.getDeparture().getDepartureTime())
                .arrivalTime(lastTransit.getArrival().getArrivalTime())
                .localizedDistanceText(textOrNull(localizedValues.path("distance"), "text"))
                .localizedDurationText(textOrNull(localizedValues.path("duration"), "text"))
                .transits(transits)
                .build();
    }


    private void collectRouteDetails(JsonNode steps, List<TrainRouteTransitDTO> collectedTransits) {
        if (!steps.isArray()) {
            return;
        }

        for (JsonNode step : steps) {
            JsonNode transitDetails = step.path("transitDetails");
            if (transitDetails.isMissingNode()) {
                continue;
            }

            JsonNode stopDetails = transitDetails.path("stopDetails");
            JsonNode transitLine = transitDetails.path("transitLine");
            TrainRouteTransitDTO transit = mapTransit(stopDetails, transitLine);
            addTransitIfValid(collectedTransits, transit);
        }
    }


    private TrainRouteTransitDTO mapTransit(JsonNode stopDetails, JsonNode transitLine) {
        TrainRouteStopDTO departureStop = mapDepartureStop(stopDetails);
        TrainRouteStopDTO arrivalStop = mapArrivalStop(stopDetails);

        return TrainRouteTransitDTO.builder()
                .departure(departureStop)
                .arrival(arrivalStop)
                .trainName(resolveTrainName(transitLine))
                .vehicleType(textOrNull(transitLine.path("vehicle").path("name"), "text"))
                .agencyName(firstAgencyName(transitLine.path("agencies")))
                .build();
    }


    private TrainRouteStopDTO mapDepartureStop(JsonNode stopDetails) {
        return TrainRouteStopDTO.builder()
                .stationName(stopDetails.path("departureStop").path("name").asText(null))
                .departureTime(textOrNull(stopDetails, "departureTime"))
                .build();
    }


    private TrainRouteStopDTO mapArrivalStop(JsonNode stopDetails) {
        return TrainRouteStopDTO.builder()
                .stationName(stopDetails.path("arrivalStop").path("name").asText(null))
                .arrivalTime(textOrNull(stopDetails, "arrivalTime"))
                .build();
    }


    private void addTransitIfValid(List<TrainRouteTransitDTO> transits, TrainRouteTransitDTO candidate) {
        if (candidate.getDeparture() == null || candidate.getDeparture().getStationName() == null
            || candidate.getDeparture().getStationName().isBlank()) {
            return;
        }
        if (candidate.getArrival() == null || candidate.getArrival().getStationName() == null
            || candidate.getArrival().getStationName().isBlank()) {
            return;
        }
        transits.add(candidate);
    }


    private String resolveTrainName(JsonNode transitLine) {
        String shortName = textOrNull(transitLine, "nameShort");
        return shortName != null && !shortName.isBlank() ? shortName : textOrNull(transitLine, "name");
    }


    private String firstAgencyName(JsonNode agencies) {
        if (!agencies.isArray() || agencies.isEmpty()) {
            return null;
        }
        return textOrNull(agencies.get(0), "name");
    }


    private String textOrNull(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        return value.isMissingNode() || value.isNull() ? null : value.asText();
    }


    private void enrichTransitsWithStationInfo(List<TrainRouteTransitDTO> transits) {
        // Collect all unique station names to avoid duplicate API calls
        Map<String, TrainRouteStopDTO> queryToFirstStop = new java.util.LinkedHashMap<>();
        List<TrainRouteStopDTO> allStops = new java.util.ArrayList<>();

        for (TrainRouteTransitDTO transit : transits) {
            allStops.add(transit.getDeparture());
            allStops.add(transit.getArrival());
        }

        for (TrainRouteStopDTO stop : allStops) {
            if (stop == null || stop.getStationName() == null || stop.getStationName().isBlank()) {
                continue;
            }
            String query = stop.getStationName().replace("Hauptbahnhof", "Hbf");
            queryToFirstStop.putIfAbsent(query, stop);
        }

        // Search each unique station name once and cache result
        Map<String, StationDTO> queryToStation = new java.util.HashMap<>();
        Map<Long, List<FacilityDTO>> numberToFacilities = new java.util.HashMap<>();

        for (String query : queryToFirstStop.keySet()) {
            try {
                List<StationDTO> results = stationService.searchStations(query);
                if (results == null || results.isEmpty()) {
                    log.warn("No station found for query: {}", query);
                    continue;
                }
                StationDTO station = results.getFirst();
                queryToStation.put(query, station);

                if (station.getNumber() != null && !numberToFacilities.containsKey(station.getNumber())) {
                    try {
                        numberToFacilities.put(station.getNumber(), stationService.getStationFacilities(station.getNumber()));
                    } catch (Exception e) {
                        log.warn("Could not load facilities for station number {}: {}", station.getNumber(), e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.warn("Could not enrich stop '{}' with station info: {}", query, e.getMessage());
            }
        }

        // Apply results to all stops
        for (TrainRouteStopDTO stop : allStops) {
            if (stop == null || stop.getStationName() == null || stop.getStationName().isBlank()) {
                continue;
            }
            String query = stop.getStationName().replace("Hauptbahnhof", "Hbf");
            StationDTO station = queryToStation.get(query);
            if (station != null) {
                stop.setStation(station);
                if (station.getNumber() != null) {
                    stop.setFacilities(numberToFacilities.get(station.getNumber()));
                }
            }
        }
    }

}
