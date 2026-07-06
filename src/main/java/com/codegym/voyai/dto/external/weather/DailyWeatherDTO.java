package com.codegym.voyai.dto.external.weather;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DailyWeatherDTO {
    private String date;
    private Double tempMax;
    private Double tempMin;
    private Double precipitation;
    private Integer weatherCode;
    private String condition;      // "Nắng", "Mây", "Mưa nhẹ"...
    private String icon;           // "sunny", "cloudy", "rainy"...
    private String recommendation; // Gợi ý hoạt động
    private Boolean isRainy;
    private Double windSpeed;
}