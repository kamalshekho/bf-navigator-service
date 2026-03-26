package com.bf.navigator.service.station.client;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;


@Component
public class StationDataClient {

    private final RestTemplate restTemplate;
    private final String clientId;
    private final String clientSecret;
    private final ObjectMapper objectMapper;
    private static final String baseUrl = "https://apis.deutschebahn.com/db-api-marketplace/apis/station-data/v2";


    public StationDataClient(RestTemplate restTemplate,
            @Value("${db.client-id}") String clientId,
            @Value("${db.client-secret}") String clientSecret,
            ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.objectMapper = objectMapper;
    }


    public ArrayNode searchStations(String query) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("DB-Client-ID", clientId);
        headers.set("DB-Api-Key", clientSecret);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
                .path("/stations")
                .queryParam("searchstring", query)
                .build()
                .encode()
                .toUri();
        ResponseEntity<String> response = restTemplate.exchange(uri, HttpMethod.GET, entity, String.class);

        try {
            var rootNode = objectMapper.readTree(response.getBody());
            return (ArrayNode) rootNode.get("result");
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse StaDa response", e);
        }
    }


    public JsonNode getStationById(Long id) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("DB-Client-ID", clientId);
        headers.set("DB-Api-Key", clientSecret);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = baseUrl + "/stations/" + id;
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        try {
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            return rootNode.path("result").get(0);
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch station " + id + " from StaDa API", e);
        }
    }

}
