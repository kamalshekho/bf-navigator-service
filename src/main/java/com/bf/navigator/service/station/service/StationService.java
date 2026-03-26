package com.bf.navigator.service.station.service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Service;

import com.bf.navigator.service.station.client.StationDataClient;
import com.bf.navigator.service.station.client.StationFacilitiesClient;
import com.bf.navigator.service.station.dto.FacilityDTO;
import com.bf.navigator.service.station.dto.StationDTO;
import com.bf.navigator.service.station.mapper.FacilityMapper;
import com.bf.navigator.service.station.mapper.StationMapper;
import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;


@Service
@RequiredArgsConstructor
public class StationService {

    private final StationDataClient stationDataClient;
    private final StationFacilitiesClient stationFacilitiesClient;
    private final StationMapper stationMapper;
    private final FacilityMapper facilityMapper;


    public List<StationDTO> searchStations(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query must not be empty");
        }

        var rootNode = stationDataClient.searchStations(query);
        if (rootNode == null) {
            return List.of();
        }

        return StreamSupport.stream(rootNode.spliterator(), false)
                .map(stationMapper::stationJsonToDto)
                .filter(dto -> dto.getEvaNumber() != null)
                .collect(Collectors.toList());
    }


    public StationDTO getStationById(Long id) {
        if (id == null || id <= 0) {
            throw new IllegalArgumentException("Invalid station id: " + id);
        }

        JsonNode node = stationDataClient.getStationById(id);
        if (node == null) {
            throw new RuntimeException("Station not found with id: " + id);
        }
        StationDTO dto = stationMapper.stationJsonToDto(node);
        if (dto.getEvaNumber() == null) {
            throw new RuntimeException("Station " + id + " has no EVA number");
        }
        return dto;
    }


    public List<FacilityDTO> getStationFacilities(Long stationNumber) {
        if (stationNumber == null || stationNumber <= 0) {
            throw new IllegalArgumentException("Invalid station number: " + stationNumber);
        }

        var rootNode = stationFacilitiesClient.getStationWithFacilitiesJson(stationNumber);
        if (rootNode == null) {
            return List.of();
        }

        return StreamSupport.stream(rootNode.spliterator(), false)
                .map(facilityMapper::facilityJsonToDto)
                .collect(Collectors.toList());
    }

}
