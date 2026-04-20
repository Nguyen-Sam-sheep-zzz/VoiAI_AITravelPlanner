package com.codegym.voyai.service;

import com.codegym.voyai.model.dto.weather.DailyWeatherDTO;
import com.codegym.voyai.model.dto.weather.OpenMeteoResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class WeatherService {

    private final WebClient webClient;

    public WeatherService(WebClient.Builder builder) {
        this.webClient = builder
                .baseUrl("https://api.open-meteo.com")
                .build();
    }

    // Lấy dự báo 7 ngày cho tọa độ
    public List<DailyWeatherDTO> getForecast(double lat, double lng) {
        try {
            log.info("Fetching weather for lat={}, lng={}", lat, lng);

            OpenMeteoResponse response = webClient.get()
                    .uri(uri -> uri
                            .path("/v1/forecast")
                            .queryParam("latitude", lat)
                            .queryParam("longitude", lng)
                            .queryParam("daily",
                                    "temperature_2m_max," +
                                            "temperature_2m_min," +
                                            "precipitation_sum," +
                                            "weathercode," +
                                            "windspeed_10m_max")
                            .queryParam("timezone", "Asia/Ho_Chi_Minh")
                            .queryParam("forecast_days", 7)
                            .build())
                    .retrieve()
                    .bodyToMono(OpenMeteoResponse.class)
                    .timeout(Duration.ofSeconds(10))
                    .block();

            if (response == null || response.getDaily() == null) {
                log.warn("Open-Meteo trả về null");
                return List.of();
            }

            return parseDailyForecast(response.getDaily());

        } catch (Exception e) {
            log.error("Weather fetch error: {}", e.getMessage());
            return List.of();
        }
    }

    // Lấy weather cho 1 ngày cụ thể theo date string "2025-05-01"
    public DailyWeatherDTO getWeatherForDate(double lat, double lng, String date) {
        List<DailyWeatherDTO> forecast = getForecast(lat, lng);
        return forecast.stream()
                .filter(d -> date.equals(d.getDate()))
                .findFirst()
                .orElse(null);
    }

    // Build weather context string để đưa vào Gemini prompt
    public String buildWeatherContext(List<DailyWeatherDTO> forecast, int days) {
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

    private List<DailyWeatherDTO> parseDailyForecast(OpenMeteoResponse.DailyData daily) {
        List<DailyWeatherDTO> result = new ArrayList<>();

        if (daily.getTime() == null) return result;

        for (int i = 0; i < daily.getTime().size(); i++) {
            int code = getOrDefault(daily.getWeatherCode(), i, 0);
            double precipitation = getOrDefault(daily.getPrecipitationSum(), i, 0.0);

            result.add(DailyWeatherDTO.builder()
                    .date(daily.getTime().get(i))
                    .tempMax(getOrDefault(daily.getTemperatureMax(), i, 25.0))
                    .tempMin(getOrDefault(daily.getTemperatureMin(), i, 20.0))
                    .precipitation(precipitation)
                    .weatherCode(code)
                    .condition(getCondition(code))
                    .icon(getIcon(code))
                    .recommendation(getRecommendation(code))
                    .isRainy(precipitation > 1.0 || isRainyCode(code))
                    .windSpeed(getOrDefault(daily.getWindSpeedMax(), i, 0.0))
                    .build());
        }

        return result;
    }

    // WMO Weather Code → tên tiếng Việt
    // Tham khảo: https://open-meteo.com/en/docs#weathervariables
    private String getCondition(int code) {
        if (code == 0)              return "Trời quang";
        if (code <= 2)              return "Ít mây";
        if (code == 3)              return "Nhiều mây";
        if (code <= 49)             return "Sương mù";
        if (code <= 55)             return "Mưa phùn";
        if (code <= 65)             return "Mưa";
        if (code <= 67)             return "Mưa + lạnh";
        if (code <= 77)             return "Tuyết";
        if (code <= 82)             return "Mưa rào";
        if (code <= 84)             return "Mưa rào nặng";
        if (code <= 99)             return "Giông bão";
        return "Không xác định";
    }

    private String getIcon(int code) {
        if (code == 0)              return "sunny";
        if (code <= 2)              return "partly_cloudy";
        if (code == 3)              return "cloudy";
        if (code <= 49)             return "foggy";
        if (code <= 67)             return "rainy";
        if (code <= 77)             return "snowy";
        if (code <= 82)             return "rainy";
        if (code <= 99)             return "stormy";
        return "unknown";
    }

    private String getRecommendation(int code) {
        if (code == 0 || code <= 2)
            return "Thời tiết đẹp! Lý tưởng cho hoạt động ngoài trời và tham quan.";
        if (code == 3)
            return "Trời nhiều mây nhưng vẫn ổn. Có thể tham quan bình thường.";
        if (code <= 49)
            return "Sương mù, tầm nhìn hạn chế. Lưu ý khi di chuyển.";
        if (code <= 55)
            return "Mưa phùn nhẹ. Mang theo ô, ưu tiên điểm có mái che.";
        if (code <= 65)
            return "Có mưa. Nên ưu tiên hoạt động trong nhà: bảo tàng, mua sắm.";
        if (code <= 82)
            return "Mưa rào. Lên kế hoạch dự phòng trong nhà.";
        return "Thời tiết xấu. Ưu tiên ở trong khách sạn hoặc trung tâm thương mại.";
    }

    private boolean isRainyCode(int code) {
        return code >= 51; // Từ mưa phùn trở lên
    }

    @SuppressWarnings("unchecked")
    private <T> T getOrDefault(List<T> list, int index, T defaultVal) {
        if (list == null || index >= list.size() || list.get(index) == null)
            return defaultVal;
        return list.get(index);
    }
}