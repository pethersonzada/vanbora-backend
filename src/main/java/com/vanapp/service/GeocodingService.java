package com.vanapp.service;

import java.io.IOException;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class GeocodingService {
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    private static final String ESTADO_PAIS = ", PE, Brasil";

    public double[] geocodificarEndereco(String logradouro, String cidade) {
        if (logradouro == null || logradouro.isBlank()) {
            throw new IllegalArgumentException("O logradouro é obrigatório e não pode ser nulo ou vazio.");
        }
        if (cidade == null || cidade.isBlank()) {
            throw new IllegalArgumentException("A cidade é obrigatória e não pode ser nula ou vazia.");
        }

        double[] coords = executarGeocodificacao(logradouro, cidade, true);
        if (coords != null) return coords;

        coords = executarGeocodificacao(logradouro, cidade, false);
        if (coords != null) return coords;

        String logradouroLimpo = logradouro
                .replaceAll("(?i)(nº|num|número|apto|apt|bloco|bl|casa|quadra|qd|lt).*$", "")
                .trim();
        
        if (!logradouroLimpo.equalsIgnoreCase(logradouro.trim()) && !logradouroLimpo.isBlank()) {
            coords = executarGeocodificacao(logradouroLimpo, cidade, false);
            if (coords != null) return coords;
        }

        throw new RuntimeException("Endereço não localizado na base de dados espacial: " + logradouro + ", " + cidade);
    }

    private double[] executarGeocodificacao(String logradouro, String cidade, boolean usarBounded) {
        String enderecoBlindado = logradouro.trim() + ", " + cidade.trim() + ESTADO_PAIS;

        try {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl("https://nominatim.openstreetmap.org/search")
                    .queryParam("q", enderecoBlindado)
                    .queryParam("format", "json")
                    .queryParam("limit", 1);

            if (usarBounded) {
                builder.queryParam("viewbox", "-36.50,-8.00,-35.50,-8.50")
                       .queryParam("bounded", 1);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("User-Agent", "vanapp-faculdade/1.0");
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                builder.toUriString(), HttpMethod.GET, entity, String.class
            );

            JsonNode node = objectMapper.readTree(response.getBody());
            if (node.isArray() && node.size() > 0) {
                double lat = node.get(0).get("lat").asDouble();
                double lon = node.get(0).get("lon").asDouble();
                return new double[]{lat, lon};
            }
        } catch (Exception e) {
        }
        return null;
    }
}