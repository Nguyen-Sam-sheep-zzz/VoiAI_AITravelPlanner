package com.codegym.voyai.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;

@Slf4j
@Service
public class NominatimService {

    private final WebClient webClient;

    public NominatimService(WebClient.Builder builder) {
        this.webClient = builder
                .baseUrl("https://nominatim.openstreetmap.org")
                .defaultHeader("User-Agent", "VoyAI/1.0")
                .build();
    }

    // Tìm kiếm địa điểm — dùng cho autocomplete
    public List<NominatimResult> search(String query) {
        try {
            return webClient.get()
                    .uri(uri -> uri.path("/search")
                            .queryParam("q", query)
                            .queryParam("format", "json")
                            .queryParam("limit", 5)
                            .queryParam("addressdetails", 1)
                            .build())
                    .retrieve()
                    .bodyToFlux(NominatimResult.class)
                    .collectList()
                    .block();
        } catch (Exception e) {
            log.error("Nominatim search error: {}", e.getMessage());
            return List.of();
        }
    }

    // Lấy chi tiết 1 địa điểm theo place_id
    public NominatimResult getPlaceDetails(String placeId) {
        try {
            List<NominatimResult> results = webClient.get()
                    .uri(uri -> uri.path("/search")
                            .queryParam("place_id", placeId)
                            .queryParam("format", "json")
                            .queryParam("addressdetails", 1)
                            .build())
                    .retrieve()
                    .bodyToFlux(NominatimResult.class)
                    .collectList()
                    .block();

            return (results != null && !results.isEmpty()) ? results.get(0) : null;
        } catch (Exception e) {
            log.error("Nominatim getPlaceDetails error: {}", e.getMessage());
            return null;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NominatimResult {

        @JsonProperty("place_id")
        private Long placeId;

        @JsonProperty("osm_id")
        private Long osmId;

        @JsonProperty("display_name")
        private String displayName;

        private String lat;
        private String lon;
        private String type;
        private String importance;

        // Lấy tên thành phố từ display_name (phần đầu tiên)
        public String getCityName() {
            if (displayName == null) return "";
            return displayName.split(",")[0].trim();
        }
    }
}
