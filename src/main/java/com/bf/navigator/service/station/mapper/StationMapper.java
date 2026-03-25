package com.bf.navigator.service.station.mapper;

import org.springframework.stereotype.Component;

import com.bf.navigator.service.station.dto.StationDTO;
import com.fasterxml.jackson.databind.JsonNode;

@Component
public class StationMapper {

    public StationDTO stationJsonToDto(JsonNode node) {
        StationDTO dto = new StationDTO();
        dto.setName(node.path("name").asText(null));
        dto.setNumber(node.path("number").isMissingNode() ? null : node.path("number").asLong());
        dto.setEvaNumber(getMainEvaNumber(node));
        dto.setCity(node.path("mailingAddress").path("city").asText(null));
        dto.setCategory(node.path("category").asInt());
        dto.setHasSteplessAccess(node.path("hasSteplessAccess").asText(null));
        dto.setHasMobilityService(node.path("hasMobilityService").asText(null));
        dto.setHasWiFi(node.path("hasWiFi").asBoolean());
        return dto;
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
