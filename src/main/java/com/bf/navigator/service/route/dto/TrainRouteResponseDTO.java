package com.bf.navigator.service.route.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainRouteResponseDTO {
    private String origin;
    private String destination;
    private String departureTime;
    private String arrivalTime;
    private String localizedDistanceText;
    private String localizedDurationText;
    private List<TrainRouteTransitDTO> transits;
}
