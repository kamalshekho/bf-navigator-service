package com.bf.navigator.service.station.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.bf.navigator.service.station.dto.StationDTO;
import com.fasterxml.jackson.databind.JsonNode;

@Mapper(componentModel = "spring")
public interface StationMapper {

    @Mapping(target = "number", source = "number")
    @Mapping(target = "name", source = "name")
    @Mapping(target = "city", source = "mailingAddress.city")
    @Mapping(target = "evaNumber", expression = "java(getMainEvaNumber(stationNode))")
    StationDTO stationJsonToDto(JsonNode stationNode);

    default Long getMainEvaNumber(JsonNode stationNode) {
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
