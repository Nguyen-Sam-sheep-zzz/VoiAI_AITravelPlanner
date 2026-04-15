package com.codegym.voyai.service;

import com.codegym.voyai.model.dto.gemini.GeminiRequest;
import com.codegym.voyai.model.dto.gemini.GeminiResponse;
import com.codegym.voyai.model.dto.travel.TravelItinerary;
import com.codegym.voyai.model.dto.weather.WeatherResponse;
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

            if (placeDetails == null) {
                log.error("Không tìm thấy place details cho placeId={}", placeId);
                throw new IllegalArgumentException(
                        "Không tìm thấy thông tin địa điểm cho điểm đến đã chọn"
                );
            }

            double lat = Double.parseDouble(placeDetails.getLat());
            double lng = Double.parseDouble(placeDetails.getLon());

            // 2. Lấy dự báo thời tiết
            WeatherResponse weatherForecast = weatherService.getForecast(lat, lng);
            String weatherContext = buildWeatherContext(weatherForecast, days);

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
            enrichItinerary(itinerary, placeDetails, weatherForecast, startDate);

            return itinerary;

        } catch (WebClientResponseException e) {
            log.error("❌ WebClient Error - Status: {}, Body: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Lỗi HTTP " + e.getStatusCode() + ": " + e.getResponseBodyAsString());

        } catch (Exception e) {
            log.error("❌ Unexpected error: ", e);
            throw new RuntimeException("Lỗi khi tạo lịch trình: " + e.getMessage(), e);
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
            // ✅ Bỏ qua weather nếu service chưa sẵn sàng — không ảnh hưởng Gemini
            String weatherContext = "Thời tiết: Vui lòng kiểm tra dự báo trước khi đi.";
            try {
                WeatherResponse weatherForecast = weatherService.getForecast(lat, lng);
                if (weatherForecast != null) {
                    weatherContext = buildWeatherContext(weatherForecast, days);
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
                    .onStatus(
                            status -> status.equals(HttpStatus.UNAUTHORIZED),
                            clientResponse -> clientResponse.bodyToMono(String.class)
                                    .map(body -> new RuntimeException("API Key không hợp lệ: " + body))
                    )
                    .bodyToMono(GeminiResponse.class)
                    .timeout(Duration.ofMillis(timeout))
                    .block();

            log.info("✅ Gemini response received");
            return parseResponse(response);

        } catch (WebClientResponseException e) {
            log.error("❌ HTTP Error {}: {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Lỗi HTTP " + e.getStatusCode());
        } catch (Exception e) {
            log.error("❌ Error: ", e);
            throw new RuntimeException("Lỗi khi tạo lịch trình: " + e.getMessage(), e);
        }
    }

    private String buildWeatherContext(WeatherResponse forecast, int days) {
        if (forecast == null || forecast.getList() == null) {
            return "Thông tin thời tiết không khả dụng.";
        }

        StringBuilder context = new StringBuilder("DỰ BÁO THỜI TIẾT:\n");

        // Lấy thời tiết cho từng ngày
        for (int i = 0; i < Math.min(days, 5); i++) {
            LocalDate date = LocalDate.now().plusDays(i);
            WeatherResponse.WeatherForecast dayWeather = weatherService.getWeatherForDate(
                    forecast.getCity().getCoord().getLat(),
                    forecast.getCity().getCoord().getLon(),
                    date
            );

            if (dayWeather != null) {
                context.append(String.format(
                        "Ngày %d: Nhiệt độ %.1f°C, %s, Độ ẩm %d%%\n",
                        i + 1,
                        dayWeather.getMain().getTemp(),
                        dayWeather.getWeather().get(0).getDescription(),
                        dayWeather.getMain().getHumidity()
                ));
            }
        }

        return context.toString();
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
            WeatherResponse weatherForecast,
            LocalDate startDate) {

        // Thêm thông tin destination
        TravelItinerary.DestinationInfo destInfo = new TravelItinerary.DestinationInfo();
        destInfo.setPlaceId(placeDetails.getOsmId() != null ? placeDetails.getOsmId().toString() : placeDetails.getPlaceId().toString());
        destInfo.setFullName(placeDetails.getCityName());
        destInfo.setAddress(placeDetails.getDisplayName());
        destInfo.setLat(Double.parseDouble(placeDetails.getLat()));
        destInfo.setLng(Double.parseDouble(placeDetails.getLon()));

        // Nominatim không có photos, để empty list
        destInfo.setPhotos(List.of());

        itinerary.setDestinationInfo(destInfo);

        // Thêm weather summary
        if (weatherForecast != null && weatherForecast.getList() != null) {
            TravelItinerary.WeatherSummary weatherSummary = new TravelItinerary.WeatherSummary();

            double avgTemp = weatherForecast.getList().stream()
                    .mapToDouble(f -> f.getMain().getTemp())
                    .average()
                    .orElse(25.0);

            double minTemp = weatherForecast.getList().stream()
                    .mapToDouble(f -> f.getMain().getTempMin())
                    .min()
                    .orElse(20.0);

            double maxTemp = weatherForecast.getList().stream()
                    .mapToDouble(f -> f.getMain().getTempMax())
                    .max()
                    .orElse(30.0);

            weatherSummary.setAvgTemp(avgTemp);
            weatherSummary.setMinTemp(minTemp);
            weatherSummary.setMaxTemp(maxTemp);
            weatherSummary.setCondition(weatherForecast.getList().get(0).getWeather().get(0).getMain());
            weatherSummary.setHumidity(weatherForecast.getList().get(0).getMain().getHumidity());
            weatherSummary.setRecommendation(weatherService.getWeatherRecommendation(
                    weatherForecast.getList().get(0)
            ));

            itinerary.setWeatherSummary(weatherSummary);
        }

        // Thêm thông tin cho từng ngày
        for (int i = 0; i < itinerary.getItinerary().size(); i++) {
            TravelItinerary.DayItinerary day = itinerary.getItinerary().get(i);

            // Thêm date
            LocalDate dayDate = startDate.plusDays(i);
            day.setDate(dayDate.toString());

            // Thêm daily weather
            if (weatherForecast != null) {
                WeatherResponse.WeatherForecast dayWeather = weatherService.getWeatherForDate(
                        Double.parseDouble(placeDetails.getLat()),
                        Double.parseDouble(placeDetails.getLon()),
                        dayDate
                );

                if (dayWeather != null) {
                    TravelItinerary.DailyWeather dailyWeather = new TravelItinerary.DailyWeather();
                    dailyWeather.setTemp(dayWeather.getMain().getTemp());
                    dailyWeather.setCondition(dayWeather.getWeather().get(0).getMain());
                    dailyWeather.setIcon(dayWeather.getWeather().get(0).getIcon());
                    dailyWeather.setHumidity(dayWeather.getMain().getHumidity());
                    dailyWeather.setRainChance(dayWeather.getRain() != null ? dayWeather.getRain().getThreeHour() : 0);

                    day.setWeather(dailyWeather);
                }
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