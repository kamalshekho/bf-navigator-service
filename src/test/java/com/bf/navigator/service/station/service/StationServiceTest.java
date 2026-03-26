package com.bf.navigator.service.station.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.bf.navigator.service.station.client.StationDataClient;
import com.bf.navigator.service.station.client.StationFacilitiesClient;
import com.bf.navigator.service.station.dto.FacilityDTO;
import com.bf.navigator.service.station.dto.StationDTO;
import com.bf.navigator.service.station.mapper.FacilityMapper;
import com.bf.navigator.service.station.mapper.StationMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;


@ExtendWith(MockitoExtension.class)
class StationServiceTest {

    @Mock
    private StationDataClient stationDataClient;

    @Mock
    private StationFacilitiesClient stationFacilitiesClient;

    @Mock
    private StationMapper stationMapper;

    @Mock
    private FacilityMapper facilityMapper;

    @InjectMocks
    private StationService stationService;


    @Test
    void searchStationsHappyReturnsStations() {
        String query = "Hamburg";
        JsonNode jsonNode = mock(JsonNode.class);
        List<JsonNode> nodes = List.of(jsonNode);
        ArrayNode arrayNode = mock(ArrayNode.class);
        when(arrayNode.spliterator()).thenReturn(Spliterators.spliterator(nodes, Spliterator.ORDERED));
        StationDTO dto = new StationDTO();
        dto.setName("Hamburg Hbf");
        dto.setEvaNumber(8002549L);
        when(stationMapper.stationJsonToDto(jsonNode)).thenReturn(dto);
        when(stationDataClient.searchStations(query)).thenReturn(arrayNode);

        List<StationDTO> result = stationService.searchStations(query);

        verify(stationDataClient).searchStations(query);
        verify(stationMapper).stationJsonToDto(jsonNode);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Hamburg Hbf", result.get(0).getName());
    }


    @Test
    void searchStationsEmptyQueryThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> stationService.searchStations(""));
    }


    @Test
    void searchStationsNullQueryThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> stationService.searchStations(null));
    }


    @Test
    void getStationByIdValidReturnsStation() {
        Long id = 2514L;
        JsonNode jsonNode = mock(JsonNode.class);
        StationDTO dto = new StationDTO();
        dto.setName("Hamburg Hbf");
        dto.setEvaNumber(8002549L);
        when(stationDataClient.getStationById(id)).thenReturn(jsonNode);
        when(stationMapper.stationJsonToDto(jsonNode)).thenReturn(dto);

        StationDTO result = stationService.getStationById(id);

        verify(stationDataClient).getStationById(id);
        assertEquals("Hamburg Hbf", result.getName());
    }


    @Test
    void getStationByIdInvalidIdThrows() {
        assertThrows(IllegalArgumentException.class, () -> stationService.getStationById(null));
        assertThrows(IllegalArgumentException.class, () -> stationService.getStationById(0L));
        verifyNoInteractions(stationDataClient);
    }


    @Test
    void getStationByIdNoStationThrows() {
        Long id = 999L;
        when(stationDataClient.getStationById(id)).thenReturn(null);
        assertThrows(RuntimeException.class, () -> stationService.getStationById(id));
    }


    @Test
    void getStationByIdNoEvaThrows() {
        Long id = 999L;
        JsonNode jsonNode = mock(JsonNode.class);
        StationDTO dto = new StationDTO();
        dto.setEvaNumber(null);
        when(stationDataClient.getStationById(id)).thenReturn(jsonNode);
        when(stationMapper.stationJsonToDto(jsonNode)).thenReturn(dto);
        assertThrows(RuntimeException.class, () -> stationService.getStationById(id));
    }


    @Test
    void getStationFacilitiesReturnsFacilities() {
        Long stationNumber = 53L;
        String facilityDescription = "zu Gleis 1/2";

        JsonNode jsonNode = mock(JsonNode.class);
        List<JsonNode> nodes = List.of(jsonNode);
        ArrayNode arrayNode = mock(ArrayNode.class);
        FacilityDTO dto = FacilityDTO.builder().description(facilityDescription).build();

        when(arrayNode.spliterator()).thenReturn(Spliterators.spliterator(nodes, Spliterator.ORDERED));
        when(facilityMapper.facilityJsonToDto(jsonNode)).thenReturn(dto);
        when(stationFacilitiesClient.getStationWithFacilitiesJson(stationNumber)).thenReturn(arrayNode);

        List<FacilityDTO> result = stationService.getStationFacilities(stationNumber);

        verify(stationFacilitiesClient).getStationWithFacilitiesJson(stationNumber);
        verify(facilityMapper).facilityJsonToDto(jsonNode);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(facilityDescription, result.getFirst().getDescription());
    }


    @Test
    void getStationFacilitiesInvalidIdThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> stationService.getStationFacilities(null));
        assertThrows(IllegalArgumentException.class, () -> stationService.getStationFacilities(0L));
        verifyNoInteractions(stationFacilitiesClient);
    }


    @Test
    void getFacilityByIdNoFacilitiesReturnsEmptyArray() {
        Long stationNumber = 42L;

        when(stationFacilitiesClient.getStationWithFacilitiesJson(stationNumber)).thenReturn(null);

        List<FacilityDTO> result = stationService.getStationFacilities(stationNumber);

        assertEquals(0, result.size());
    }

}
