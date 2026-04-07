package com.bf.navigator.service.station.controller;

import com.bf.navigator.service.station.dto.StationDTO;
import com.bf.navigator.service.station.dto.FacilityDTO;
import com.bf.navigator.service.station.service.StationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class StationControllerIntegrationTest {

  @Mock
  private StationService stationService;

  @InjectMocks
  private StationController stationController;

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
    mockMvc = MockMvcBuilders.standaloneSetup(stationController)
        .setMessageConverters(new MappingJackson2HttpMessageConverter())
        .build();
  }

  @Test
  void testSearchStations() throws Exception {
    StationDTO station = new StationDTO("Hamburg Hbf", 2514L, 8002549L, "Hamburg", 2, "yes", "yes", true);
    when(stationService.searchStations(eq("Hamburg"))).thenReturn(List.of(station));

    mockMvc.perform(get("/stations/search")
        .param("query", "Hamburg")
        .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].name").value("Hamburg Hbf"))
        .andExpect(jsonPath("$[0].city").value("Hamburg"));
  }

  @Test
  void testGetStation() throws Exception {
    StationDTO station = new StationDTO("Hamburg Hbf", 2514L, 8002549L, "Hamburg", 2, "yes", "yes", true);
    when(stationService.getStationById(eq(2514L))).thenReturn(station);

    mockMvc.perform(get("/stations/2514")
        .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Hamburg Hbf"))
        .andExpect(jsonPath("$.number").value(2514));
  }

  @Test
  void testGetStationFacilities() throws Exception {
    FacilityDTO f1 = FacilityDTO.builder().description("zu Gleis 1/2").stationnumber(53L).build();
    FacilityDTO f2 = FacilityDTO.builder().description("zu Gleis 3/4").stationnumber(53L).build();

    when(stationService.getStationFacilities(eq(53L))).thenReturn(List.of(f1, f2));

    mockMvc.perform(get("/stations/53/facilities")
        .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$[0].description").value("zu Gleis 1/2"))
        .andExpect(jsonPath("$[1].description").value("zu Gleis 3/4"));
  }
}