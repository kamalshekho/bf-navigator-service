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
    void searchStations_callsService_returnsOk() {
        StationDTO dto = new StationDTO("Test", 1L, 1L, "Test");
        when(stationService.searchStations("test")).thenReturn(List.of(dto));

        ResponseEntity<List<StationDTO>> result = stationController.searchStations("test");

        verify(stationService).searchStations("test");
        assertNotNull(result.getBody());
        assertFalse(result.getBody().isEmpty());
    }
}
