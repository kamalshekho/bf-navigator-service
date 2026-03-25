package com.bf.navigator.service.timetable.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class TimetableClient {

    private final RestTemplate restTemplate;
    private final String clientId;
    private final String apiKey;
    private final String baseUrl = "https://apis.deutschebahn.com/db-api-marketplace/apis/timetables/v1";

    public TimetableClient(RestTemplate restTemplate,
            @Value("${db.client-id}") String clientId,
            @Value("${db.client-secret}") String clientSecret) {
        this.restTemplate = restTemplate;
        this.clientId = clientId;
        this.apiKey = clientSecret;
    }

    private static final Logger log = LoggerFactory.getLogger(TimetableClient.class);

    public String getTimetableRaw(Long evaNumber, String date, String hour) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", "application/xml");
        headers.set("DB-Client-ID", clientId);
        headers.set("DB-Api-Key", apiKey);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = baseUrl + "/plan/" + evaNumber + "/" + date + "/" + hour;
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        String xml = response.getBody();
        log.info("Timetable XML for eva={}, date={}, hour={}: length={}, first100={}", evaNumber, date, hour,
                xml != null ? xml.length() : 0, xml != null ? xml.substring(0, Math.min(100, xml.length())) : "null");
        return xml;
    }
}
