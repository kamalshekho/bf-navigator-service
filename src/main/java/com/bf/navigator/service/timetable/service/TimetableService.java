package com.bf.navigator.service.timetable.service;

import com.bf.navigator.service.timetable.client.TimetableClient;
import com.bf.navigator.service.timetable.dto.TimetableDTO;
import com.bf.navigator.service.timetable.mapper.TimetableMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TimetableService {

    private final TimetableClient timetableClient;
    private final TimetableMapper timetableMapper;

    private static final Logger log = LoggerFactory.getLogger(TimetableService.class);

    public List<TimetableDTO> getTimetable(Long evaNumber, String date, String hour) {
        if (evaNumber == null || evaNumber <= 0) {
            throw new IllegalArgumentException("Invalid EVA number: " + evaNumber);
        }
        if (date == null || date.length() != 6 || hour == null || hour.length() != 2) {
            throw new IllegalArgumentException("Invalid date or hour format");
        }

        String xml = timetableClient.getTimetableRaw(evaNumber, date, hour);
        if (xml == null || xml.trim().isEmpty()) {
            log.warn("Empty XML response for eva={}, date={}, hour={}", evaNumber, date, hour);
            return List.of();
        }

        List<TimetableDTO> result = timetableMapper.parseTimetables(xml);
        log.info("Parsed {} timetable entries for eva={}, date={}, hour={}", result.size(), evaNumber, date, hour);
        return result;
    }
}
