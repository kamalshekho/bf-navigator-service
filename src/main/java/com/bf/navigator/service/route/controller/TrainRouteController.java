package com.bf.navigator.service.route.controller;

import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bf.navigator.service.route.dto.TrainRouteRequestDTO;
import com.bf.navigator.service.route.dto.TrainRouteResponseDTO;
import com.bf.navigator.service.route.service.TrainRouteService;

import lombok.RequiredArgsConstructor;


@RestController
@RequestMapping("/routes/trains")
@RequiredArgsConstructor
public class TrainRouteController {

    private final TrainRouteService trainRouteService;


    @PostMapping
    public ResponseEntity<TrainRouteResponseDTO> getTrainRoute(@Valid @RequestBody TrainRouteRequestDTO request) {
        return ResponseEntity.ok(trainRouteService.getTrainRoute(request, false));
    }


    @PostMapping(value = "/original", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> getTrainRouteRaw(@Valid @RequestBody TrainRouteRequestDTO request) {
        return ResponseEntity.ok(trainRouteService.getTrainRouteRaw(request));
    }


    @PostMapping(value = "/debug", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<TrainRouteResponseDTO> getDebugTrainRoute(@Valid @RequestBody TrainRouteRequestDTO request) {
        return ResponseEntity.ok(trainRouteService.getTrainRoute(request, true));
    }

}
