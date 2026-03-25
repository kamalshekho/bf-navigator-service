package com.bf.navigator.service.station.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Spliterator;
import java.util.Spliterators;

import com.bf.navigator.service.station.client.StaDaClient;
import com.bf.navigator.service.station.dto.StationDTO;
import com.bf.navigator.service.station.mapper.StationMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class StationServiceTest {

    @Mock
    private StaDaClient staDaClient;

    @Mock
    private StationMapper stationMapper;

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
        when(staDaClient.searchStations(query)).thenReturn(arrayNode);

        List<StationDTO> result = stationService.searchStations(query);

        verify(staDaClient).searchStations(query);
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
        when(staDaClient.getStationById(id)).thenReturn(jsonNode);
        when(stationMapper.stationJsonToDto(jsonNode)).thenReturn(dto);

        StationDTO result = stationService.getStationById(id);

        verify(staDaClient).getStationById(id);
        assertEquals("Hamburg Hbf", result.getName());
    }

    @Test
    void getStationByIdInvalidIdThrows() {
        assertThrows(IllegalArgumentException.class, () -> stationService.getStationById(null));
        assertThrows(IllegalArgumentException.class, () -> stationService.getStationById(0L));
        verifyNoInteractions(staDaClient);
    }

    @Test
    void getStationByIdNoStationThrows() {
        Long id = 999L;
        when(staDaClient.getStationById(id)).thenReturn(null);
        assertThrows(RuntimeException.class, () -> stationService.getStationById(id));
    }

    @Test
    void getStationByIdNoEvaThrows() {
        Long id = 999L;
        JsonNode jsonNode = mock(JsonNode.class);
        StationDTO dto = new StationDTO();
        dto.setEvaNumber(null);
        when(staDaClient.getStationById(id)).thenReturn(jsonNode);
        when(stationMapper.stationJsonToDto(jsonNode)).thenReturn(dto);
        assertThrows(RuntimeException.class, () -> stationService.getStationById(id));
    }
}
