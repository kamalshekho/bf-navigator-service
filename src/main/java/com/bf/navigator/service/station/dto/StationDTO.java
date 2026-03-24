package com.bf.navigator.service.station.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StationDTO {
    private String name;
    private Long number;
    private Long evaNumber;
    private String city;
}