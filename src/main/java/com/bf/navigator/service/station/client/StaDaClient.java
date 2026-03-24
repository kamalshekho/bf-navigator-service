package com.bf.navigator.service.station.client;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

@Component
public class StaDaClient {

    private final WebClient webClient;
    private final String clientId;
    private final String clientSecret;

    public StaDaClient(WebClient webClient,
            @Value("${db.client-id}") String clientId,
            @Value("${db.client-secret}") String clientSecret) {
        this.webClient = webClient;
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public String searchStations(String query) {
        try {
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/stations")
                            .queryParam("searchstring", query)
                            .build())
                    .header("DB-Client-ID", clientId)
                    .header("DB-Api-Key", clientSecret)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
        } catch (WebClientResponseException e) {
            System.err
                    .println("Error calling StaDa API: " + e.getStatusCode() + " - " + e.getResponseBodyAsString());
            throw new RuntimeException("Failed to fetch stations from StaDa API", e);
        }
    }
}