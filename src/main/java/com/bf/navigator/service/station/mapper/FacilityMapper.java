package com.bf.navigator.service.station.mapper;

import org.springframework.stereotype.Component;

import com.bf.navigator.service.station.dto.FacilityDTO;
import com.bf.navigator.service.station.dto.FacilityState;
import com.bf.navigator.service.station.dto.FacilityType;
import com.fasterxml.jackson.databind.JsonNode;


@Component
public class FacilityMapper {

    public FacilityDTO facilityJsonToDto(JsonNode node) {
        FacilityDTO dto = new FacilityDTO();
        dto.setDescription(node.path("description").asText(null));
        dto.setEquipmentnumber(node.path("equipmentnumber").isMissingNode() ? null : node.path("equipmentnumber").asLong());
        dto.setGeocoordX(node.path("geocoordX").isMissingNode() ? null : node.path("geocoordX").asDouble());
        dto.setGeocoordY(node.path("geocoordY").isMissingNode() ? null : node.path("geocoordY").asDouble());
        dto.setOperationalResumeDate(node.path("operationalResumeDate").asText(null));
        dto.setOperatorname(node.path("operatorname").asText(null));
        dto.setState(node.path("state").isMissingNode() ? null : FacilityState.valueOf(node.path("state").asText()));
        dto.setStateExplanation(node.path("stateExplanation").asText(null));
        dto.setStationnumber(node.path("stationnumber").isMissingNode() ? null : node.path("stationnumber").asLong());
        dto.setType(node.path("type").isMissingNode() ? null : FacilityType.valueOf(node.path("type").asText()));
        return dto;
    }

}
