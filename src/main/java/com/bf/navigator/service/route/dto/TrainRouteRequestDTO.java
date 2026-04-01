package com.bf.navigator.service.route.dto;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainRouteRequestDTO {

    @NotBlank
    private String origin;

    @NotBlank
    private String destination;

    @NotNull
    private OffsetDateTime departureTime;
}

