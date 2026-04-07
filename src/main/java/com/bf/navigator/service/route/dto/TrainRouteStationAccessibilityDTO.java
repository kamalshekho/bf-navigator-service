package com.bf.navigator.service.route.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainRouteStationAccessibilityDTO {
    private String status;
    private String summary;
    private boolean stepFreeAvailable;
    private boolean mobilityServiceAvailable;
    private boolean hasFacilityData;
    private int activeElevators;
    private int inactiveElevators;
    private int activeEscalators;
    private int inactiveEscalators;

    public boolean getHasFacilityData() {
        return hasFacilityData;
    }
}
