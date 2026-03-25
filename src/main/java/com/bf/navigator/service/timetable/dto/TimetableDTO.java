package com.bf.navigator.service.timetable.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TimetableDTO {
    private String trainNumber;
    private String trainType;
    private String line;
    private String departureTime;
    private String arrivalTime;
    private String departurePlatform;
    private String arrivalPlatform;
    private List<String> route;
}
