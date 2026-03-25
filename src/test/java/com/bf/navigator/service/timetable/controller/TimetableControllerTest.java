package com.bf.navigator.service.timetable.controller;

import com.bf.navigator.service.timetable.dto.TimetableDTO;
import com.bf.navigator.service.timetable.service.TimetableService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimetableControllerTest {

    @Mock
    private TimetableService timetableService;

    @InjectMocks
    private TimetableController timetableController;

    @Test
    void getTimetableCallsServiceReturnsOk() {
        Long evaNumber = 8002549L;
        String date = "260325";
        String time = "0900";
        List<TimetableDTO> mockDtos = List.of(
                new TimetableDTO("123", "ICE", "09:02", null, "5", null, null, List.of("Berlin")));

        when(timetableService.getTimetable(evaNumber, date, "09")).thenReturn(mockDtos);

        ResponseEntity<List<TimetableDTO>> result = timetableController.getTimetable(evaNumber, date, time);

        assertTrue(result.getStatusCode().is2xxSuccessful());
        assertEquals(mockDtos, result.getBody());
        verify(timetableService).getTimetable(evaNumber, date, "09");
    }
}
