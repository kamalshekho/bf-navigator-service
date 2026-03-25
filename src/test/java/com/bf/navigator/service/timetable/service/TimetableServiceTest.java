package com.bf.navigator.service.timetable.service;

import com.bf.navigator.service.timetable.client.TimetableClient;
import com.bf.navigator.service.timetable.dto.TimetableDTO;
import com.bf.navigator.service.timetable.mapper.TimetableMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TimetableServiceTest {

    @Mock
    private TimetableClient timetableClient;

    @Mock
    private TimetableMapper timetableMapper;

    @InjectMocks
    private TimetableService timetableService;

    @Test
    void getTimetableValidInputReturnsList() {
        Long evaNumber = 8002549L;
        String date = "260325";
        String hour = "09";
        String mockXml = "<mock-xml/>";
        List<TimetableDTO> mockDtos = List.of(
                new TimetableDTO("123", "ICE", "09:02", "09:00", "5", "4", null, List.of("Berlin")));

        when(timetableClient.getTimetableRaw(evaNumber, date, hour)).thenReturn(mockXml);
        when(timetableMapper.parseTimetables(mockXml)).thenReturn(mockDtos);

        List<TimetableDTO> result = timetableService.getTimetable(evaNumber, date, hour);

        assertEquals(mockDtos, result);
        verify(timetableClient).getTimetableRaw(evaNumber, date, hour);
        verify(timetableMapper).parseTimetables(mockXml);
    }

    @Test
    void getTimetableEmptyXmlReturnsEmptyList() {
        Long evaNumber = 8002549L;
        String date = "260325";
        String hour = "09";
        when(timetableClient.getTimetableRaw(evaNumber, date, hour)).thenReturn("");

        List<TimetableDTO> result = timetableService.getTimetable(evaNumber, date, hour);

        assertTrue(result.isEmpty());
    }

    @Test
    void getTimetableInvalidEvaThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            timetableService.getTimetable(0L, "260325", "09");
        });
    }

    @Test
    void getTimetableInvalidDateThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> {
            timetableService.getTimetable(8002549L, "invalid", "09");
        });
    }
}
