package com.bf.navigator.service.station.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class FacilityDTO {

    private String description;
    private Long equipmentnumber;
    private Double geocoordX;
    private Double geocoordY;
    private String operationalResumeDate; // format: YYYY-MM-DD, only for INACTIVE (ISO 8601 date format)
    private String operatorname;
    private FacilityState state;
    private String stateExplanation;
    private Long stationnumber;
    private FacilityType type;

}
