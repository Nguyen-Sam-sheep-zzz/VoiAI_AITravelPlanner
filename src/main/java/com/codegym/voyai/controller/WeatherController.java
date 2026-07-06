package com.codegym.voyai.controller;

import com.codegym.voyai.dto.external.weather.DailyWeatherDTO;
import com.codegym.voyai.service.WeatherService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/public/weather")
@RequiredArgsConstructor
public class WeatherController {

    private final WeatherService weatherService;

    // Frontend gọi để hiển thị weather trên trip detail page
    @GetMapping("/forecast")
    public ResponseEntity<?> getForecast(
            @RequestParam double lat,
            @RequestParam double lng) {
        List<DailyWeatherDTO> forecast = weatherService.getForecast(lat, lng);
        if (forecast.isEmpty()) {
            return ResponseEntity.ok(Map.of(
                    "message", "Không lấy được dự báo thời tiết",
                    "data", List.of()
            ));
        }
        return ResponseEntity.ok(forecast);
    }
}