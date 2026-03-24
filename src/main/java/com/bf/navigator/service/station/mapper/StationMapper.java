package com.bf.navigator.service.station.mapper;

import org.springframework.stereotype.Component;

import com.bf.navigator.service.station.dto.StationDTO;
import com.fasterxml.jackson.databind.JsonNode;

@Component
public class StationMapper {

    public StationDTO stationJsonToDto(JsonNode stationNode) {
        if (stationNode == null)
            return null;

        StationDTO dto = new StationDTO();
        dto.setNumber(stationNode.has("number") ? stationNode.get("number").asLong() : null);
        dto.setName(stationNode.has("name") ? stationNode.get("name").asText() : null);
        dto.setCity(stationNode.has("mailingAddress") && stationNode.get("mailingAddress").has("city")
                ? stationNode.get("mailingAddress").get("city").asText()
                : null);
        dto.setEvaNumber(getMainEvaNumber(stationNode));
        return dto;
    }

    private Long getMainEvaNumber(JsonNode stationNode) {
        if (stationNode.has("evaNumbers") && stationNode.get("evaNumbers").isArray()) {
            for (JsonNode eva : stationNode.get("evaNumbers")) {
                if (eva.has("isMain") && eva.get("isMain").asBoolean()) {
                    return eva.get("number").asLong();
                }
            }
        }
        return null;
    }
}
