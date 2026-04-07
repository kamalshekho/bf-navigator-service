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
public class TrainRouteSearchResponseDTO {
    private List<TrainRouteResponseDTO> trips;
}
