package com.bf.navigator.service.station.mapper;

import org.springframework.stereotype.Component;

import com.bf.navigator.service.station.dto.StationDTO;
import com.fasterxml.jackson.databind.JsonNode;

@Component
public class StationMapper {

    public StationDTO stationJsonToDto(JsonNode node) {
        return new StationDTO(
                node.path("name").asText(null),
                node.path("number").isMissingNode() ? null : node.path("number").asLong(),
                getMainEvaNumber(node),
                node.path("mailingAddress").path("city").asText(null));
    }

    private Long getMainEvaNumber(JsonNode node) {
        for (JsonNode eva : node.path("evaNumbers")) {
            if (eva.path("isMain").asBoolean(false)) {
                return eva.path("number").asLong();
            }
        }
        return null;
    }
}
