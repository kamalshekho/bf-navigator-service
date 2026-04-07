package com.bf.navigator.service.route.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WalkingApproachDTO {
    private String instruction;
    private Double latitude;
    private Double longitude;
}
