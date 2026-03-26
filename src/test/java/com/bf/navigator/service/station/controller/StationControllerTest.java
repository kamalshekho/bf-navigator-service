package com.bf.navigator.service.station.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import com.bf.navigator.service.station.dto.FacilityDTO;
import com.bf.navigator.service.station.dto.StationDTO;
import com.bf.navigator.service.station.service.StationService;

import lombok.NonNull;


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


    @Test
    void getStationFacilitiesReturnsOk() {
        Long stationNumber = 53L;
        List<FacilityDTO> facilities = new ArrayList<>();
        facilities.add(FacilityDTO.builder().description("zu Gleis 1/2").build());
        facilities.add(FacilityDTO.builder().description("zu Gleis 3/4").build());
        facilities.add(FacilityDTO.builder().description("zu Gleis 5/6").build());

        when(stationService.getStationFacilities(stationNumber)).thenReturn(facilities);

        ResponseEntity<@NonNull List<FacilityDTO>> result = stationController.getStationFacilities(stationNumber);

        verify(stationService).getStationFacilities(stationNumber);
        assertNotNull(result.getBody());
        assertEquals("zu Gleis 1/2", result.getBody().get(0).getDescription());
        assertEquals("zu Gleis 3/4", result.getBody().get(1).getDescription());
        assertEquals("zu Gleis 5/6", result.getBody().get(2).getDescription());
    }

}
