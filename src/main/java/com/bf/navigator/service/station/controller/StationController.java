package com.bf.navigator.service.station.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bf.navigator.service.station.dto.StationDTO;
import com.bf.navigator.service.station.service.StationService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class StationController {

    private final StationService stationService;

    @GetMapping("/stations/search")
    public ResponseEntity<List<StationDTO>> searchStations(@RequestParam String query) {
        List<StationDTO> stations = stationService.searchStations(query);
        return ResponseEntity.ok(stations);
    }
}
