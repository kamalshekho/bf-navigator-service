package com.bf.navigator.service.route.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import jakarta.validation.Valid;

import org.jspecify.annotations.Nullable;
import org.springframework.stereotype.Service;

import com.bf.navigator.service.route.client.GoogleTrainRouteClient;
import com.bf.navigator.service.route.dto.TrainRouteRequestDTO;
import com.bf.navigator.service.route.dto.TrainRouteAccessibilitySummaryDTO;
import com.bf.navigator.service.route.dto.TrainRouteResponseDTO;
import com.bf.navigator.service.route.dto.TrainRouteSearchResponseDTO;
import com.bf.navigator.service.route.dto.TrainRouteStationAccessibilityDTO;
import com.bf.navigator.service.route.dto.TrainRouteStopDTO;
import com.bf.navigator.service.route.dto.TrainRouteTouchpointDTO;
import com.bf.navigator.service.route.dto.TrainRouteTransitDTO;
import com.bf.navigator.service.route.dto.WalkingApproachDTO;
import com.bf.navigator.service.station.dto.FacilityState;
import com.bf.navigator.service.station.dto.FacilityDTO;
import com.bf.navigator.service.station.dto.FacilityType;
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

    private record MappedTransitStep(TrainRouteTransitDTO transit, @Nullable WalkingApproachDTO walkingApproach) {
    }

    private final GoogleTrainRouteClient googleTrainRouteClient;
    private final StationService stationService;


    public TrainRouteSearchResponseDTO searchTrainRoutes(TrainRouteRequestDTO request, boolean debug) {
        validateRequest(request);

        ArrayNode routes = googleTrainRouteClient.computeTrainRoutes(request, debug);
        if (routes == null || routes.isEmpty()) {
            throw new RuntimeException("No train route found for the provided origin and destination");
        }

        List<TrainRouteResponseDTO> mappedRoutes = new java.util.ArrayList<>();
        for (JsonNode route : routes) {
            TrainRouteResponseDTO mappedRoute = mapRoute(route);
            if (mappedRoute != null) {
                mappedRoutes.add(mappedRoute);
            }
        }

        if (mappedRoutes.isEmpty()) {
            throw new RuntimeException("Google Routes response did not contain train stop details");
        }

        return TrainRouteSearchResponseDTO.builder()
                .trips(mappedRoutes)
                .build();
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

        List<MappedTransitStep> mappedTransitSteps = new java.util.ArrayList<>();
        for (JsonNode leg : legs) {
            collectRouteDetails(leg.path("steps"), mappedTransitSteps);
        }

        if (mappedTransitSteps.isEmpty()) {
            return null;
        }

        List<TrainRouteTransitDTO> transits = mappedTransitSteps.stream()
                .map(MappedTransitStep::transit)
                .toList();

        enrichTransitsWithStationInfo(transits);

        JsonNode localizedValues = route.path("localizedValues");
        TrainRouteTransitDTO firstTransit = transits.getFirst();
        TrainRouteTransitDTO lastTransit = transits.getLast();
        List<TrainRouteTouchpointDTO> touchpoints = buildTouchpoints(mappedTransitSteps);

        return TrainRouteResponseDTO.builder()
                .origin(firstTransit.getDeparture().getStationName())
                .destination(lastTransit.getArrival().getStationName())
                .departureTime(firstTransit.getDeparture().getDepartureTime())
                .arrivalTime(lastTransit.getArrival().getArrivalTime())
                .localizedDistanceText(textOrNull(localizedValues.path("distance"), "text"))
                .localizedDurationText(textOrNull(localizedValues.path("duration"), "text"))
                .accessibilitySummary(buildAccessibilitySummary(transits))
                .transits(transits)
                .touchpoints(touchpoints)
                .build();
    }

    private List<TrainRouteTouchpointDTO> buildTouchpoints(List<MappedTransitStep> mappedTransitSteps) {
        List<TrainRouteTouchpointDTO> touchpoints = new java.util.ArrayList<>();
        if (mappedTransitSteps.isEmpty()) {
            return touchpoints;
        }

        MappedTransitStep firstTransit = mappedTransitSteps.getFirst();
        appendOrMergeTouchpoint(
                touchpoints,
                firstTransit.transit().getDeparture(),
                firstTransit.walkingApproach(),
                true);

        for (int index = 0; index < mappedTransitSteps.size(); index++) {
            TrainRouteTransitDTO transit = mappedTransitSteps.get(index).transit();
            appendOrMergeTouchpoint(touchpoints, transit.getArrival(), null, false);
            if (index < mappedTransitSteps.size() - 1) {
                appendOrMergeTouchpoint(touchpoints, mappedTransitSteps.get(index + 1).transit().getDeparture(),
                        mappedTransitSteps.get(index + 1).walkingApproach(),
                        true);
            }
        }

        for (int index = 0; index < touchpoints.size(); index++) {
            TrainRouteTouchpointDTO touchpoint = touchpoints.get(index);
            touchpoint.setKind(resolveTouchpointKind(index, touchpoints.size()));
            touchpoint.setAccessibility(buildTouchpointAccessibility(touchpoint.getStation(), touchpoint.getFacilities()));
        }

        return touchpoints;
    }

    private void appendOrMergeTouchpoint(List<TrainRouteTouchpointDTO> touchpoints,
            TrainRouteStopDTO stop,
            @Nullable WalkingApproachDTO walkingApproach,
            boolean isDeparture) {
        if (stop == null || stop.getStationName() == null || stop.getStationName().isBlank()) {
            return;
        }

        TrainRouteTouchpointDTO candidate = TrainRouteTouchpointDTO.builder()
                .stationName(stop.getStationName())
                .arrivalTime(stop.getArrivalTime())
                .departureTime(stop.getDepartureTime())
                .station(stop.getStation())
                .facilities(stop.getFacilities())
                .departureStop(isDeparture ? stop : null)
                .arrivalStop(isDeparture ? null : stop)
                .walkingApproach(isDeparture ? walkingApproach : null)
                .build();

        if (touchpoints.isEmpty()) {
            touchpoints.add(candidate);
            return;
        }

        TrainRouteTouchpointDTO previous = touchpoints.getLast();
        if (!sameStation(previous, candidate)) {
            touchpoints.add(candidate);
            return;
        }

        if (candidate.getArrivalTime() != null) {
            previous.setArrivalTime(candidate.getArrivalTime());
        }
        if (candidate.getDepartureTime() != null) {
            previous.setDepartureTime(candidate.getDepartureTime());
        }
        if (previous.getStation() == null && candidate.getStation() != null) {
            previous.setStation(candidate.getStation());
        }
        if (candidate.getFacilities() != null) {
            previous.setFacilities(candidate.getFacilities());
        }
        if (previous.getDepartureStop() == null && candidate.getDepartureStop() != null) {
            previous.setDepartureStop(candidate.getDepartureStop());
        }
        if (previous.getArrivalStop() == null && candidate.getArrivalStop() != null) {
            previous.setArrivalStop(candidate.getArrivalStop());
        }
        if (previous.getWalkingApproach() == null && candidate.getWalkingApproach() != null) {
            previous.setWalkingApproach(candidate.getWalkingApproach());
        }
    }

    private boolean sameStation(TrainRouteTouchpointDTO left, TrainRouteTouchpointDTO right) {
        if (left.getStation() != null && right.getStation() != null
            && left.getStation().getEvaNumber() != null
            && right.getStation().getEvaNumber() != null) {
            return Objects.equals(left.getStation().getEvaNumber(), right.getStation().getEvaNumber());
        }

        return Objects.equals(left.getStationName(), right.getStationName());
    }

    private String resolveTouchpointKind(int index, int totalTouchpoints) {
        if (index == 0) {
            return "ORIGIN";
        }
        if (index == totalTouchpoints - 1) {
            return "DESTINATION";
        }
        return "TRANSFER";
    }

    private TrainRouteStationAccessibilityDTO buildTouchpointAccessibility(@Nullable StationDTO station,
            @Nullable List<FacilityDTO> facilities) {
        boolean stepFreeAvailable = station != null && "yes".equalsIgnoreCase(station.getHasSteplessAccess());
        boolean mobilityServiceAvailable = station != null && "yes".equalsIgnoreCase(station.getHasMobilityService());
        boolean hasFacilityData = facilities != null;

        List<FacilityDTO> safeFacilities = facilities == null ? List.of() : facilities;
        int activeElevators = countFacilities(safeFacilities, FacilityType.ELEVATOR, FacilityState.ACTIVE);
        int inactiveElevators = countFacilities(safeFacilities, FacilityType.ELEVATOR, FacilityState.INACTIVE);
        int activeEscalators = countFacilities(safeFacilities, FacilityType.ESCALATOR, FacilityState.ACTIVE);
        int inactiveEscalators = countFacilities(safeFacilities, FacilityType.ESCALATOR, FacilityState.INACTIVE);

        return TrainRouteStationAccessibilityDTO.builder()
                .status(resolveTouchpointAccessibilityStatus(station, stepFreeAvailable, mobilityServiceAvailable,
                        hasFacilityData, activeElevators, inactiveElevators, activeEscalators, inactiveEscalators))
                .summary(buildTouchpointAccessibilitySummary(station, stepFreeAvailable, mobilityServiceAvailable,
                        hasFacilityData, safeFacilities.size(), activeElevators, inactiveElevators, activeEscalators,
                        inactiveEscalators))
                .stepFreeAvailable(stepFreeAvailable)
                .mobilityServiceAvailable(mobilityServiceAvailable)
                .hasFacilityData(hasFacilityData)
                .activeElevators(activeElevators)
                .inactiveElevators(inactiveElevators)
                .activeEscalators(activeEscalators)
                .inactiveEscalators(inactiveEscalators)
                .build();
    }

    private String resolveTouchpointAccessibilityStatus(@Nullable StationDTO station, boolean stepFreeAvailable,
            boolean mobilityServiceAvailable, boolean hasFacilityData, int activeElevators, int inactiveElevators,
            int activeEscalators, int inactiveEscalators) {
        if (station == null && !hasFacilityData) {
            return "UNKNOWN";
        }

        if (stepFreeAvailable && inactiveElevators == 0 && inactiveEscalators == 0) {
            return "ACCESSIBLE";
        }

        if (stepFreeAvailable || mobilityServiceAvailable || hasFacilityData
            || activeElevators > 0 || inactiveElevators > 0 || activeEscalators > 0 || inactiveEscalators > 0) {
            return "LIMITED";
        }

        return "UNKNOWN";
    }

    private String buildTouchpointAccessibilitySummary(@Nullable StationDTO station, boolean stepFreeAvailable,
            boolean mobilityServiceAvailable, boolean hasFacilityData, int facilityCount, int activeElevators,
            int inactiveElevators, int activeEscalators, int inactiveEscalators) {
        if (station == null && !hasFacilityData) {
            return "Station accessibility data unavailable";
        }

        List<String> summaryParts = new java.util.ArrayList<>();

        if (stepFreeAvailable) {
            summaryParts.add("Step-free access available");
        } else if (station != null) {
            summaryParts.add("No confirmed step-free access");
        }

        if (mobilityServiceAvailable) {
            summaryParts.add("Mobility service available");
        }

        if (!hasFacilityData) {
            summaryParts.add("No live facility data");
        } else if (facilityCount == 0) {
            summaryParts.add("No listed elevators or escalators");
        } else {
            if (activeElevators + inactiveElevators > 0) {
                summaryParts.add("Elevators " + activeElevators + " active / " + inactiveElevators + " inactive");
            }
            if (activeEscalators + inactiveEscalators > 0) {
                summaryParts.add("Escalators " + activeEscalators + " active / " + inactiveEscalators + " inactive");
            }
        }

        return String.join(" · ", summaryParts);
    }


    private TrainRouteAccessibilitySummaryDTO buildAccessibilitySummary(List<TrainRouteTransitDTO> transits) {
        Map<String, TrainRouteStopDTO> uniqueStops = new java.util.LinkedHashMap<>();

        for (TrainRouteTransitDTO transit : transits) {
            addUniqueStop(uniqueStops, transit.getDeparture());
            addUniqueStop(uniqueStops, transit.getArrival());
        }

        List<TrainRouteStopDTO> stops = new java.util.ArrayList<>(uniqueStops.values());
        int totalStations = stops.size();
        int stepFreeStations = (int) stops.stream()
                .map(TrainRouteStopDTO::getStation)
                .filter(Objects::nonNull)
                .filter(station -> "yes".equalsIgnoreCase(station.getHasSteplessAccess()))
                .count();
        int mobilityServiceStations = (int) stops.stream()
                .map(TrainRouteStopDTO::getStation)
                .filter(Objects::nonNull)
                .filter(station -> "yes".equalsIgnoreCase(station.getHasMobilityService()))
                .count();

        List<FacilityDTO> uniqueFacilities = stops.stream()
                .map(TrainRouteStopDTO::getFacilities)
                .filter(Objects::nonNull)
                .flatMap(List::stream)
                .collect(Collectors.toMap(
                        facility -> facility.getEquipmentnumber() != null
                                ? facility.getEquipmentnumber().toString()
                                : facility.getStationnumber() + ":" + facility.getType() + ":" + facility.getDescription(),
                        facility -> facility,
                        (left, right) -> left,
                        java.util.LinkedHashMap::new))
                .values()
                .stream()
                .toList();

        int activeElevators = countFacilities(uniqueFacilities, FacilityType.ELEVATOR, FacilityState.ACTIVE);
        int inactiveElevators = countFacilities(uniqueFacilities, FacilityType.ELEVATOR, FacilityState.INACTIVE);
        int activeEscalators = countFacilities(uniqueFacilities, FacilityType.ESCALATOR, FacilityState.ACTIVE);
        int inactiveEscalators = countFacilities(uniqueFacilities, FacilityType.ESCALATOR, FacilityState.INACTIVE);

        return TrainRouteAccessibilitySummaryDTO.builder()
                .status(resolveAccessibilityStatus(totalStations, stepFreeStations, inactiveElevators, inactiveEscalators))
                .summary(stepFreeStations + "/" + totalStations + " stations step-free")
                .totalStations(totalStations)
                .stepFreeStations(stepFreeStations)
                .mobilityServiceStations(mobilityServiceStations)
                .activeElevators(activeElevators)
                .inactiveElevators(inactiveElevators)
                .activeEscalators(activeEscalators)
                .inactiveEscalators(inactiveEscalators)
                .build();
    }


    private void addUniqueStop(Map<String, TrainRouteStopDTO> uniqueStops, TrainRouteStopDTO stop) {
        if (stop == null || stop.getStationName() == null || stop.getStationName().isBlank()) {
            return;
        }

        StationDTO station = stop.getStation();
        if (station != null && station.getEvaNumber() != null) {
            uniqueStops.putIfAbsent("eva:" + station.getEvaNumber(), stop);
            return;
        }

        uniqueStops.putIfAbsent("name:" + stop.getStationName(), stop);
    }


    private int countFacilities(List<FacilityDTO> facilities, FacilityType type, FacilityState state) {
        return (int) facilities.stream()
                .filter(Objects::nonNull)
                .filter(facility -> facility.getType() == type)
                .filter(facility -> facility.getState() == state)
                .count();
    }


    private String resolveAccessibilityStatus(int totalStations, int stepFreeStations, int inactiveElevators,
            int inactiveEscalators) {
        if (totalStations > 0 && stepFreeStations == totalStations && inactiveElevators == 0 && inactiveEscalators == 0) {
            return "ACCESSIBLE";
        }

        if (stepFreeStations > 0 || inactiveElevators > 0 || inactiveEscalators > 0) {
            return "LIMITED";
        }

        return "UNKNOWN";
    }


    private void collectRouteDetails(JsonNode steps, List<MappedTransitStep> collectedTransits) {
        if (!steps.isArray()) {
            return;
        }

        WalkingApproachDTO lastWalkingApproach = null;

        for (JsonNode step : steps) {
            String travelMode = step.path("travelMode").asText();

            if ("WALK".equals(travelMode)) {
                WalkingApproachDTO walkingApproach = mapWalkingApproach(step);
                if (walkingApproach != null) {
                    lastWalkingApproach = walkingApproach;
                }
                continue;
            }

            if (!"TRANSIT".equals(travelMode)) {
                continue;
            }

            JsonNode transitDetails = step.path("transitDetails");
            if (transitDetails.isMissingNode()) {
                continue;
            }

            JsonNode stopDetails = transitDetails.path("stopDetails");
            JsonNode transitLine = transitDetails.path("transitLine");
            TrainRouteTransitDTO transit = mapTransit(stopDetails, transitLine);
            if (isValidTransit(transit)) {
                collectedTransits.add(new MappedTransitStep(transit, lastWalkingApproach));
                lastWalkingApproach = null;
            }
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
        JsonNode latLng = stopDetails.path("departureStop").path("location").path("latLng");

        return TrainRouteStopDTO.builder()
                .stationName(stopDetails.path("departureStop").path("name").asText(null))
                .departureTime(textOrNull(stopDetails, "departureTime"))
                .latitude(doubleOrNull(latLng, "latitude"))
                .longitude(doubleOrNull(latLng, "longitude"))
                .build();
    }


    private TrainRouteStopDTO mapArrivalStop(JsonNode stopDetails) {
        JsonNode latLng = stopDetails.path("arrivalStop").path("location").path("latLng");

        return TrainRouteStopDTO.builder()
                .stationName(stopDetails.path("arrivalStop").path("name").asText(null))
                .arrivalTime(textOrNull(stopDetails, "arrivalTime"))
                .latitude(doubleOrNull(latLng, "latitude"))
                .longitude(doubleOrNull(latLng, "longitude"))
                .build();
    }


    private @Nullable WalkingApproachDTO mapWalkingApproach(JsonNode step) {
        JsonNode latLng = step.path("endLocation").path("latLng");
        Double latitude = doubleOrNull(latLng, "latitude");
        Double longitude = doubleOrNull(latLng, "longitude");

        if (latitude == null || longitude == null) {
            return null;
        }

        return WalkingApproachDTO.builder()
                .instruction(sanitizeInstruction(textOrNull(step.path("navigationInstruction"), "instructions")))
                .latitude(latitude)
                .longitude(longitude)
                .build();
    }


    private boolean isValidTransit(TrainRouteTransitDTO candidate) {
        if (candidate.getDeparture() == null || candidate.getDeparture().getStationName() == null
            || candidate.getDeparture().getStationName().isBlank()) {
            return false;
        }
        if (candidate.getArrival() == null || candidate.getArrival().getStationName() == null
            || candidate.getArrival().getStationName().isBlank()) {
            return false;
        }
        return true;
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


    private Double doubleOrNull(JsonNode node, String fieldName) {
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull() || !value.isNumber()) {
            return null;
        }
        double doubleValue = value.doubleValue();
        return Double.isFinite(doubleValue) ? doubleValue : null;
    }


    private String sanitizeInstruction(String instruction) {
        if (instruction == null) {
            return null;
        }
        return instruction.replace("<", "&lt;").replace(">", "&gt;");
    }


    private String buildStationQuery(String stationName) {
        if (stationName.contains("Hauptbahnhof")) {
            String withHbf = stationName.replace("Hauptbahnhof", "Hbf");
            return stationName + "," + withHbf;
        }
        if (stationName.contains("Hbf")) {
            String withHauptbahnhof = stationName.replace("Hbf", "Hauptbahnhof");
            return stationName + "," + withHauptbahnhof;
        }
        return stationName;
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
            String query = buildStationQuery(stop.getStationName());
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
            String query = buildStationQuery(stop.getStationName());
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
