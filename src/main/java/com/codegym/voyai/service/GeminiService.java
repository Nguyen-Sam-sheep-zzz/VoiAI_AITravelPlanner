package com.codegym.voyai.service;

import com.codegym.voyai.model.dto.gemini.GeminiRequest;
import com.codegym.voyai.model.dto.gemini.GeminiResponse;
import com.codegym.voyai.model.dto.travel.TravelItinerary;
import com.codegym.voyai.model.dto.weather.DailyWeatherDTO;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

@Service
public class GeminiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiService.class);

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final WeatherService weatherService;
    private final NominatimService nominatimService; // THAY ĐỔI: Dùng Nominatim

    @Value("${gemini.api.key}")
    private String apiKey;

    @Value("${gemini.api.base-url}")
    private String baseUrl;

    @Value("${gemini.api.model}")
    private String model;

    @Value("${gemini.api.timeout:30000}")
    private int timeout;

    // CONSTRUCTOR MỚI
    public GeminiService(WebClient.Builder webClientBuilder,
                         ObjectMapper objectMapper,
                         WeatherService weatherService,
                         NominatimService nominatimService) { // Thay GoogleMapsService
        this.webClient = webClientBuilder
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();
        this.objectMapper = objectMapper;
        this.weatherService = weatherService;
        this.nominatimService = nominatimService; // THAY ĐỔI
    }

    /**
     * Tạo lịch trình với Nominatim (FREE)
     */
    public TravelItinerary generateTravelItinerary(
            String destination,
            int days,
            String budget,
            String notes,
            String placeId,
            LocalDate startDate) {

        String url = "";
        try {
            // 1. Lấy thông tin địa điểm từ NOMINATIM
            NominatimService.NominatimResult placeDetails = nominatimService.getPlaceDetails(placeId);
            if (placeDetails == null) throw new IllegalArgumentException("Không tìm thấy địa điểm");

            double lat = Double.parseDouble(placeDetails.getLat());
            double lng = Double.parseDouble(placeDetails.getLon());

            // 2. Lấy dự báo thời tiết
            List<DailyWeatherDTO> forecast = List.of();
            String weatherContext = "Thời tiết: Không có dữ liệu dự báo cụ thể.";
            try {
                forecast = weatherService.getForecast(lat, lng);
                if (!forecast.isEmpty()) {
                    weatherContext = buildWeatherContext(forecast, days);
                }
            } catch (Exception e) {
                log.warn("Lỗi lấy thời tiết: {}", e.getMessage());
            }

            // 3. Tạo prompt với context đầy đủ
            String prompt = createEnhancedPrompt(
                    destination,
                    days,
                    budget,
                    notes,
                    weatherContext,
                    lat,
                    lng
            );

            GeminiRequest.Part part = new GeminiRequest.Part(prompt);
            GeminiRequest.Content content = new GeminiRequest.Content(List.of(part));

            GeminiRequest.GenerationConfig config = new GeminiRequest.GenerationConfig(
                    0.7, 40, 0.95, 8192, "application/json"
            );

            GeminiRequest request = new GeminiRequest(List.of(content), config);

            url = String.format("%s/%s:generateContent?key=%s", baseUrl, model, apiKey);

            log.info("=== CALLING GEMINI API ===");
            log.info("Destination: {} (lat: {}, lng: {})", destination, lat, lng);
            log.info("Weather context included: YES");

            GeminiResponse response = webClient.post()
                    .uri(url)
                    .header("Content-Type", "application/json")
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(
                            status -> status.equals(HttpStatus.UNAUTHORIZED),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .map(body -> new RuntimeException("API Key không hợp lệ: " + body))
                    )
                    .bodyToMono(GeminiResponse.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            log.info("✅ Received response from Gemini");

            // 4. Parse response
            TravelItinerary itinerary = parseResponse(response);

            // 5. Enrich với thông tin Nominatim và Weather
            enrichItinerary(itinerary, placeDetails, forecast, startDate);

            return itinerary;

        } catch (WebClientResponseException e) {
            log.error("❌ WebClient Error - Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Lỗi HTTP " + e.getStatusCode() + ": " + e.getResponseBodyAsString());

        } catch (Exception e) {
            log.error("❌ Unexpected error: ", e);
            throw new RuntimeException("Lỗi khi tạo lịch trình: " + e.getMessage(), e);
        }
    }

    private void enrichSimpleItinerary(
            TravelItinerary itinerary,
            List<DailyWeatherDTO> forecast,
            double lat,
            double lng,
            String destination) {

        // Mock thông tin destination đơn giản
        TravelItinerary.DestinationInfo destInfo = new TravelItinerary.DestinationInfo();
        destInfo.setFullName(destination);
        destInfo.setLat(lat);
        destInfo.setLng(lng);
        itinerary.setDestinationInfo(destInfo);

        // Dùng lại logic enrich weather bạn đã viết
        if (forecast != null && !forecast.isEmpty()) {
            TravelItinerary.WeatherSummary summary = new TravelItinerary.WeatherSummary();
            double avgMax = forecast.stream().mapToDouble(DailyWeatherDTO::getTempMax).average().orElse(30.0);
            double avgMin = forecast.stream().mapToDouble(DailyWeatherDTO::getTempMin).average().orElse(20.0);
            summary.setAvgTemp((avgMax + avgMin) / 2);
            summary.setMinTemp(avgMin);
            summary.setMaxTemp(avgMax);
            summary.setCondition(forecast.get(0).getCondition());
            summary.setRecommendation(forecast.get(0).getRecommendation());
            itinerary.setWeatherSummary(summary);
        }

        // Gán ngày mặc định bắt đầu từ ngày mai nếu không có startDate
        LocalDate date = LocalDate.now().plusDays(1);
        for (int i = 0; i < itinerary.getItinerary().size(); i++) {
            itinerary.getItinerary().get(i).setDate(date.plusDays(i).toString());
        }
    }

    /**
     * Version đơn giản - không cần placeId
     */
    public TravelItinerary generateSimpleTravelItinerary(
            String destination,
            int days,
            String budget,
            String notes,
            double lat,
            double lng) {

        try {
            List<DailyWeatherDTO> forecast = List.of();
            String weatherContext = "Thời tiết: Vui lòng kiểm tra dự báo trước khi đi.";
            try {
                forecast = weatherService.getForecast(lat, lng);
                if (!forecast.isEmpty()) {
                    weatherContext = buildWeatherContext(forecast, days);
                }
            } catch (Exception e) {
                log.warn("Bỏ qua weather context: {}", e.getMessage());
            }

            String prompt = createEnhancedPrompt(
                    destination, days, budget, notes, weatherContext, lat, lng);

            GeminiRequest.Part part = new GeminiRequest.Part(prompt);
            GeminiRequest.Content content = new GeminiRequest.Content(List.of(part));
            GeminiRequest.GenerationConfig config = new GeminiRequest.GenerationConfig(
                    0.7, 40, 0.95, 8192, "application/json");
            GeminiRequest request = new GeminiRequest(List.of(content), config);

            String url = String.format("%s/%s:generateContent?key=%s", baseUrl, model, apiKey);

            log.info("=== CALLING GEMINI: {} ({} ngày) ===", destination, days);

            GeminiResponse response = webClient.post()
                    .uri(url)
                    .header("Content-Type", "application/json")
                    .bodyValue(request)
                    .retrieve()
                    .bodyToMono(GeminiResponse.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            log.info("✅ Gemini response received");

            TravelItinerary itinerary = parseResponse(response);

            enrichSimpleItinerary(itinerary, forecast, lat, lng, destination);
            return itinerary;

        } catch (WebClientResponseException e) {
            log.error("❌ HTTP Error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Lỗi HTTP " + e.getStatusCode());
        } catch (Exception e) {
            log.error("❌ Error: ", e);
            throw new RuntimeException("Lỗi khi tạo lịch trình: " + e.getMessage(), e);
        }
    }

    private String buildWeatherContext(List<DailyWeatherDTO> forecast, int days) {
        if (forecast == null || forecast.isEmpty()) {
            return "Thời tiết: Không lấy được dự báo, hãy lên kế hoạch linh hoạt.";
        }

        StringBuilder sb = new StringBuilder("DỰ BÁO THỜI TIẾT:\n");
        int limit = Math.min(days, forecast.size());

        for (int i = 0; i < limit; i++) {
            DailyWeatherDTO day = forecast.get(i);
            sb.append(String.format(
                    "Ngày %d (%s): %s, Cao %.1f°C / Thấp %.1f°C, Mưa %.1fmm%s\n",
                    i + 1,
                    day.getDate(),
                    day.getCondition(),
                    day.getTempMax(),
                    day.getTempMin(),
                    day.getPrecipitation(),
                    day.getIsRainy() ? " ⚠️ CÓ MƯA" : ""
            ));
        }
        return sb.toString();
    }

    private String createEnhancedPrompt(
            String destination,
            int days,
            String budget,
            String notes,
            String weatherContext,
            double lat,
            double lng) {

        return String.format("""
                        Bạn là chuyên gia lập kế hoạch du lịch chuyên nghiệp.
                        
                        THÔNG TIN CHUYẾN ĐI:
                        - Điểm đến: %s
                        - Tọa độ: lat=%f, lng=%f
                        - Số ngày: %d ngày
                        - Ngân sách: %s
                        - Ghi chú: %s
                        
                        %s
                        
                        YÊU CẦU:
                        1. Dựa vào DỰ BÁO THỜI TIẾT để đề xuất hoạt động phù hợp
                        2. Nếu có mưa → ưu tiên hoạt động trong nhà (bảo tàng, mua sắm...)
                        3. Nếu nắng nóng → tránh hoạt động ngoài trời vào 12h-15h
                        4. Mỗi ngày có 4-6 hoạt động
                        5. Tọa độ GPS PHẢI chính xác với địa điểm thực tế tại %s
                        6. Chi phí ước tính hợp lý (VNĐ)
                        7. QUAN TRỌNG: Mỗi activity BẮT BUỘC phải có "time" theo format HH:mm
                               Ví dụ: 08:00, 10:30, 13:00, 15:30, 19:00
                        Trả về JSON theo format:
                        {
                          "destination": "%s",
                          "totalDays": %d,
                          "itinerary": [
                            {
                              "day": 1,
                              "activities": [
                                {
                                  "time": "08:00",
                                  "activity": "Tên địa điểm CỤ THỂ (VD: Bảo tàng Chăm Đà Nẵng)",
                                  "lat": 16.0544,
                                  "lng": 108.2022,
                                  "estimatedCost": 50000,
                                  "reason": "Lý do (kể cả yếu tố thời tiết)"
                                }
                              ]
                            }
                          ]
                        }
                        
                        CHỈ TRẢ VỀ JSON, KHÔNG GIẢI THÍCH.
                        """,
                destination, lat, lng, days, budget, notes != null ? notes : "Không có",
                weatherContext,
                destination,
                destination, days
        );
    }

    /**
     * Enrich itinerary với thông tin Nominatim và Weather
     */
    private void enrichItinerary(
            TravelItinerary itinerary,
            NominatimService.NominatimResult placeDetails,
            List<DailyWeatherDTO> forecast, // Đổi từ WeatherResponse sang List DTO mới
            LocalDate startDate) {

        // Thêm thông tin destination
        TravelItinerary.DestinationInfo destInfo = new TravelItinerary.DestinationInfo();
        destInfo.setPlaceId(placeDetails.getOsmId() != null ? placeDetails.getOsmId().toString() : placeDetails.getPlaceId().toString());
        destInfo.setFullName(placeDetails.getDisplayName());
        destInfo.setLat(Double.parseDouble(placeDetails.getLat()));
        destInfo.setLng(Double.parseDouble(placeDetails.getLon()));
        destInfo.setPhotos(List.of());
        itinerary.setDestinationInfo(destInfo);

        // Thêm weather summary
        if (forecast != null && !forecast.isEmpty()) {
            TravelItinerary.WeatherSummary summary = new TravelItinerary.WeatherSummary();

            double avgMax = forecast.stream().mapToDouble(DailyWeatherDTO::getTempMax).average().orElse(30.0);
            double avgMin = forecast.stream().mapToDouble(DailyWeatherDTO::getTempMin).average().orElse(20.0);

            summary.setAvgTemp((avgMax + avgMin) / 2);
            summary.setMinTemp(avgMin);
            summary.setMaxTemp(avgMax);
            summary.setCondition(forecast.get(0).getCondition());
            summary.setRecommendation(forecast.get(0).getRecommendation());

            itinerary.setWeatherSummary(summary);
        }

        // Thêm thông tin cho từng ngày
        for (int i = 0; i < itinerary.getItinerary().size(); i++) {
            TravelItinerary.DayItinerary dayDto = itinerary.getItinerary().get(i);
            LocalDate dayDate = startDate.plusDays(i);
            dayDto.setDate(dayDate.toString());

            if (forecast != null) {
                // Tìm weather trùng ngày
                forecast.stream()
                        .filter(w -> w.getDate().equals(dayDate.toString()))
                        .findFirst()
                        .ifPresent(w -> {
                            TravelItinerary.DailyWeather dw = new TravelItinerary.DailyWeather();
                            dw.setTemp(w.getTempMax()); // Hoặc trung bình max/min
                            dw.setCondition(w.getCondition());
                            dw.setIcon(w.getIcon());
                            dw.setRainChance(w.getIsRainy() ? 100.0 : 0.0); // Open-Meteo daily ko có % mưa chính xác ở code của bạn
                            dayDto.setWeather(dw);
                        });
            }
        }
    }

    private TravelItinerary parseResponse(GeminiResponse response) throws Exception {
        if (response == null ||
                response.getCandidates() == null ||
                response.getCandidates().isEmpty()) {
            log.error("❌ Empty response from Gemini");
            throw new RuntimeException("Không nhận được phản hồi từ Gemini");
        }

        String jsonText = response.getCandidates().get(0)
                .getContent()
                .getParts()
                .get(0)
                .getText();

        jsonText = jsonText.replaceAll("```json\\s*", "")
                .replaceAll("```\\s*", "")
                .trim();

        log.info("📝 JSON response preview: {}...", jsonText.substring(0, Math.min(200, jsonText.length())));

        return objectMapper.readValue(jsonText, TravelItinerary.class);
    }
}