package com.bf.navigator.service.station.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;


@Component
public class StationFacilitiesClient {

    private final RestTemplate restTemplate;
    private final String clientId;
    private final String clientSecret;
    private final ObjectMapper objectMapper;
    private static final String baseUrl = "https://apis.deutschebahn.com/db-api-marketplace/apis/fasta/v2/";


    public StationFacilitiesClient(RestTemplate restTemplate,
            @Value("${db.client-id}") String clientId,
            @Value("${db.client-secret}") String clientSecret,
            ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.objectMapper = objectMapper;
    }


    public ArrayNode getStationWithFacilitiesJson(Long stationNumber) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("DB-Client-ID", clientId);
        headers.set("DB-Api-Key", clientSecret);
        HttpEntity<String> entity = new HttpEntity<>(headers);

        String url = baseUrl + "/stations/" + stationNumber;
        ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.GET, entity, String.class);

        try {
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            return (ArrayNode) rootNode.get("facilities");
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch station facilities" + stationNumber + " from Fasta API", e);
        }
    }

}
