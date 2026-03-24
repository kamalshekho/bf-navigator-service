package com.bf.navigator.service.station.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

@Component
public class StaDaClient {

    private final WebClient webClient;
    private final String clientId;
    private final String clientSecret;
    private final ObjectMapper objectMapper;

    public StaDaClient(WebClient webClient,
            @Value("${db.client-id}") String clientId,
            @Value("${db.client-secret}") String clientSecret,
            ObjectMapper objectMapper) {
        this.webClient = webClient;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.objectMapper = objectMapper;
    }

    public ArrayNode searchStations(String query) {
        try {
            String response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/stations")
                            .queryParam("searchstring", query)
                            .build())
                    .header("DB-Client-ID", clientId)
                    .header("DB-Api-Key", clientSecret)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode rootNode = objectMapper.readTree(response);

            if (!rootNode.isArray()) {
                throw new RuntimeException("Expected JSON array from StaDa API");
            }

            return (ArrayNode) rootNode;
        } catch (Exception e) {
            throw new RuntimeException("Failed to fetch stations from StaDa API", e);
        }
    }
}