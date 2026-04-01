package com.bf.navigator.service.route.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainRouteTransitDTO {

    private TrainRouteStopDTO departure;
    private TrainRouteStopDTO arrival;
    private String trainName;
    private String vehicleType;
    private String agencyName;

}
