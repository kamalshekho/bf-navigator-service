package com.bf.navigator.service.station.client;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;

class StationDataClientTest {

    @Test
    void searchStationsBuildsEncodedUriForQueriesWithSpaces() {
        RestTemplate restTemplate = Mockito.mock(RestTemplate.class);
        ObjectMapper objectMapper = new ObjectMapper();
        StationDataClient stationDataClient = new StationDataClient(
                restTemplate,
                "client-id",
                "client-secret",
                objectMapper);

        when(restTemplate.exchange(any(URI.class), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(ResponseEntity.ok("{\"result\":[]}"));

        ArrayNode result = stationDataClient.searchStations("Berlin Hb*");

        ArgumentCaptor<URI> uriCaptor = ArgumentCaptor.forClass(URI.class);
        verify(restTemplate).exchange(uriCaptor.capture(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class));

        URI requestedUri = uriCaptor.getValue();
        assertNotNull(result);
        assertNotNull(requestedUri);
        assertFalse(requestedUri.toString().contains(" "));
        assertFalse(requestedUri.getRawQuery().contains(" "));
    }
}
