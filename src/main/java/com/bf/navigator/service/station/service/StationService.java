package com.bf.navigator.service.station.service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import org.springframework.stereotype.Service;

import com.bf.navigator.service.station.client.StaDaClient;
import com.bf.navigator.service.station.dto.StationDTO;
import com.bf.navigator.service.station.mapper.StationMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class StationService {

    private final StaDaClient staDaClient;
    private final StationMapper stationMapper;

    public List<StationDTO> searchStations(String query) {
        if (query == null || query.isBlank()) {
            throw new IllegalArgumentException("Query must not be empty");
        }

        var rootNode = staDaClient.searchStations(query);

        if (rootNode == null) {
            return List.of();
        }

        return StreamSupport.stream(rootNode.spliterator(), false)
                .map(stationMapper::stationJsonToDto)
                .filter(dto -> dto.getEvaNumber() != null)
                .collect(Collectors.toList());
    }
}
