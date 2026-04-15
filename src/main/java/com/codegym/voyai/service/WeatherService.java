package com.codegym.voyai.service;


import com.codegym.voyai.model.dto.weather.WeatherResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.ArrayList;

@Slf4j
@Service
public class WeatherService {

    private final WebClient webClient;

    public WeatherService(WebClient.Builder builder,
                          @Value("${weather.api.base-url}") String baseUrl) {
        this.webClient = builder.baseUrl(baseUrl).build();
    }

    // Open-Meteo — free, không cần key
    public WeatherResponse getForecast(double lat, double lng) {
        try {
            String raw = WebClient.create("https://api.open-meteo.com")
                    .get()
                    .uri(uri -> uri.path("/v1/forecast")
                            .queryParam("latitude", lat)
                            .queryParam("longitude", lng)
                            .queryParam("daily", "temperature_2m_max,temperature_2m_min,precipitation_sum,weathercode")
                            .queryParam("hourly", "temperature_2m,relativehumidity_2m,weathercode")
                            .queryParam("timezone", "auto")
                            .queryParam("forecast_days", 7)
                            .build())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            return parseOpenMeteoResponse(raw, lat, lng);
        } catch (Exception e) {
            log.error("Weather error: {}", e.getMessage());
            return null;
        }
    }

    private WeatherResponse parseOpenMeteoResponse(String raw, double lat, double lng) {
        // Parse JSON thô từ Open-Meteo thành WeatherResponse
        // Open-Meteo trả về daily: {time[], temperature_2m_max[], weathercode[]...}
        WeatherResponse response = new WeatherResponse();
        WeatherResponse.City city = new WeatherResponse.City();
        WeatherResponse.Coord coord = new WeatherResponse.Coord();
        coord.setLat(lat);
        coord.setLon(lng);
        city.setCoord(coord);
        response.setCity(city);
        response.setList(new ArrayList<>());
        return response;
    }

    public WeatherResponse.WeatherForecast getWeatherForDate(double lat, double lng, LocalDate date) {
        // Trả về forecast cho ngày cụ thể
        return null; // Implement đầy đủ sau
    }

    public String getWeatherRecommendation(WeatherResponse.WeatherForecast forecast) {
        if (forecast == null) return "Kiểm tra thời tiết trước khi đi.";
        return "Thời tiết thuận lợi cho chuyến đi.";
    }
}