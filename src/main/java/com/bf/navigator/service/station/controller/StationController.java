package com.bf.navigator.service.station.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bf.navigator.service.station.dto.FacilityDTO;
import com.bf.navigator.service.station.dto.StationDTO;
import com.bf.navigator.service.station.service.StationService;

import lombok.NonNull;
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


    @GetMapping("/stations/{id}")
    public ResponseEntity<StationDTO> getStation(@PathVariable Long id) {
        StationDTO station = stationService.getStationById(id);
        return ResponseEntity.ok(station);
    }


    @GetMapping("/stations/{stationNumber}/facilities")
    public ResponseEntity<@NonNull List<FacilityDTO>> getStationFacilities(@PathVariable Long stationNumber) {
        List<FacilityDTO> facilities = stationService.getStationFacilities(stationNumber);
        return ResponseEntity.ok(facilities);
    }

}
