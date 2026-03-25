package com.bf.navigator.service.timetable.controller;

import com.bf.navigator.service.timetable.dto.TimetableDTO;
import com.bf.navigator.service.timetable.service.TimetableService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TimetableController {

    private final TimetableService timetableService;

    @GetMapping("/stations/{evaNumber}/timetable")
    public ResponseEntity<List<TimetableDTO>> getTimetable(
            @PathVariable Long evaNumber,
            @RequestParam String date,
            @RequestParam String time) {
        String hour = time.length() >= 2 ? time.substring(0, 2) : time;
        List<TimetableDTO> timetables = timetableService.getTimetable(evaNumber, date, hour);
        return ResponseEntity.ok(timetables);
    }
}
