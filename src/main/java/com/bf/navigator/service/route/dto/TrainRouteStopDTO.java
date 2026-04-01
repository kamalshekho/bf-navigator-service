package com.bf.navigator.service.route.dto;

import com.bf.navigator.service.station.dto.FacilityDTO;
import com.bf.navigator.service.station.dto.StationDTO;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainRouteStopDTO {
    private String stationName;
    private String arrivalTime;
    private String departureTime;

    // Station info from DB API
    private StationDTO station;
    private List<FacilityDTO> facilities;
}
