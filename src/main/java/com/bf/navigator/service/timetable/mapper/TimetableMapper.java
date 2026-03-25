package com.bf.navigator.service.timetable.mapper;

import com.bf.navigator.service.timetable.dto.TimetableDTO;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Component
public class TimetableMapper {

    private final XmlMapper xmlMapper = new XmlMapper();

    public List<TimetableDTO> parseTimetables(String xml) {
        try {
            JsonNode root = xmlMapper.readTree(xml);

            List<TimetableDTO> result = new ArrayList<>();

            JsonNode trains = root.path("s");

            if (trains.isArray()) {
                for (JsonNode s : trains) {
                    TimetableDTO dto = parseTrain(s);
                    if (dto.getTrainType() != null) {
                        result.add(dto);
                    }
                }
            }

            return result;

        } catch (Exception e) {
            throw new RuntimeException("Failed to parse timetable XML", e);
        }
    }

    private TimetableDTO parseTrain(JsonNode s) {
        TimetableDTO dto = new TimetableDTO();

        JsonNode tl = s.path("tl");
        dto.setTrainType(tl.path("c").asText(null));
        dto.setTrainNumber(tl.path("n").asText(null));

        JsonNode dp = s.path("dp");
        dto.setLine(dp.path("l").asText(null));
        dto.setDepartureTime(formatTime(dp.path("pt").asText(null)));
        dto.setDeparturePlatform(dp.path("pp").asText(null));
        dto.setRoute(parseRoute(dp.path("ppth").asText(null)));

        JsonNode ar = s.path("ar");
        dto.setArrivalTime(formatTime(ar.path("pt").asText(null)));
        dto.setArrivalPlatform(ar.path("pp").asText(null));

        if (dto.getRoute() == null || dto.getRoute().isEmpty()) {
            dto.setRoute(parseRoute(ar.path("ppth").asText(null)));
        }

        return dto;
    }

    private String formatTime(String time) {
        if (time == null || time.length() < 10)
            return null;

        String hh = time.substring(6, 8);
        String mm = time.substring(8, 10);

        return hh + ":" + mm;
    }

    private List<String> parseRoute(String ppth) {
        if (ppth == null || ppth.isEmpty())
            return List.of();
        return Arrays.asList(ppth.split("\\|"));
    }
}
