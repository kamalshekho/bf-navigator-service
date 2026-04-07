package com.bf.navigator.service.route.dto;

import java.util.List;

import com.bf.navigator.service.station.dto.FacilityDTO;
import com.bf.navigator.service.station.dto.StationDTO;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainRouteTouchpointDTO {
    private String kind;
    private String stationName;
    private String arrivalTime;
    private String departureTime;
    private StationDTO station;
    private List<FacilityDTO> facilities;
    private TrainRouteStationAccessibilityDTO accessibility;
    private TrainRouteStopDTO departureStop;
    private TrainRouteStopDTO arrivalStop;
    private WalkingApproachDTO walkingApproach;
}
