package com.bf.navigator.service.route.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainRouteAccessibilitySummaryDTO {
    private String status;
    private String summary;
    private int totalStations;
    private int stepFreeStations;
    private int mobilityServiceStations;
    private int activeElevators;
    private int inactiveElevators;
    private int activeEscalators;
    private int inactiveEscalators;
}
