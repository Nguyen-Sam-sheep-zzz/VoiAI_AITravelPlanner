package com.codegym.voyai.model.dto.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class OpenMeteoResponse {

    private Double latitude;
    private Double longitude;
    private String timezone;

    @JsonProperty("daily")
    private DailyData daily;

    @JsonProperty("hourly")
    private HourlyData hourly;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class DailyData {
        private List<String> time;

        @JsonProperty("temperature_2m_max")
        private List<Double> temperatureMax;

        @JsonProperty("temperature_2m_min")
        private List<Double> temperatureMin;

        @JsonProperty("precipitation_sum")
        private List<Double> precipitationSum;

        @JsonProperty("weathercode")
        private List<Integer> weatherCode;

        @JsonProperty("windspeed_10m_max")
        private List<Double> windSpeedMax;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class HourlyData {
        private List<String> time;

        @JsonProperty("temperature_2m")
        private List<Double> temperature;

        @JsonProperty("relativehumidity_2m")
        private List<Integer> humidity;

        @JsonProperty("precipitation_probability")
        private List<Integer> precipitationProbability;
    }
}
