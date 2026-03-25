package com.bf.navigator.service.station.controller;

import static org.mockito.Mockito.*;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import com.bf.navigator.service.station.dto.StationDTO;
import com.bf.navigator.service.station.service.StationService;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class StationControllerTest {

    @Mock
    private StationService stationService;

    @InjectMocks
    private StationController stationController;

    @Test
    void searchStationsCallsServiceReturnsOk() {
        StationDTO dto = new StationDTO();
        dto.setName("Test");
        dto.setNumber(1L);
        dto.setEvaNumber(1L);
        dto.setCity("Test");
        when(stationService.searchStations("test")).thenReturn(List.of(dto));

        ResponseEntity<List<StationDTO>> result = stationController.searchStations("test");

        verify(stationService).searchStations("test");
        assertNotNull(result.getBody());
        assertFalse(result.getBody().isEmpty());
    }

    @Test
    void getStationCallsServiceReturnsOk() {
        StationDTO dto = new StationDTO();
        dto.setName("Hamburg Hbf");
        dto.setNumber(2514L);
        dto.setEvaNumber(8002549L);
        dto.setCity("Hamburg");
        dto.setCategory(2);
        dto.setHasSteplessAccess("yes");
        dto.setHasMobilityService("yes");
        dto.setHasWiFi(true);
        when(stationService.getStationById(2514L)).thenReturn(dto);

        ResponseEntity<StationDTO> result = stationController.getStation(2514L);

        verify(stationService).getStationById(2514L);
        assertNotNull(result.getBody());
        assertEquals("Hamburg Hbf", result.getBody().getName());
    }
}
