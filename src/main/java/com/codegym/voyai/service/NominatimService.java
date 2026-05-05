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
            // Sử dụng endpoint /details thay vì /search hoặc /lookup
            return webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/details")
                            .queryParam("place_id", placeId)
                            .queryParam("format", "json")
                            .build())
                    .header("User-Agent", "VoyAI-Travel-Planner-Student-Project") // Bắt buộc phải có
                    .retrieve()
                    .bodyToMono(NominatimResult.class) // /details trả về 1 Object duy nhất
                    .block();
        } catch (Exception e) {
            // In log chi tiết để dễ debug sau này
            log.error("Nominatim API Error for placeId {}: {}", placeId, e.getMessage());
            return null;
        }
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class NominatimResult {
        @JsonProperty("place_id")
        private Long placeId;

        @JsonProperty("osm_id") // Thêm dòng này
        private Long osmId;

        @JsonProperty("display_name")
        private String displayName;

        private String lat;
        private String lon;

        @JsonProperty("centroid")
        private Centroid centroid;

        @Data
        @JsonIgnoreProperties(ignoreUnknown = true)
        public static class Centroid {
            private List<Double> coordinates;
        }

        // Hàm lấy Lat/Lon an toàn như đã hướng dẫn trước đó
        public String getLat() {
            if (lat != null) return lat;
            if (centroid != null && centroid.getCoordinates() != null && centroid.getCoordinates().size() > 1) {
                return String.valueOf(centroid.getCoordinates().get(1));
            }
            return null;
        }

        public String getLon() {
            if (lon != null) return lon;
            if (centroid != null && centroid.getCoordinates() != null && centroid.getCoordinates().size() > 0) {
                return String.valueOf(centroid.getCoordinates().get(0));
            }
            return null;
        }
    }

}
