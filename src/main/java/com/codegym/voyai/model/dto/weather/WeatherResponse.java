package com.codegym.voyai.model.dto.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherResponse {

    private List<WeatherForecast> list;
    private City city;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WeatherForecast {
        private Main main;
        private List<Weather> weather;
        private Rain rain;
        private String dtTxt;
    }

    @Data
    public static class Main {
        private Double temp;
        private Double tempMin;
        private Double tempMax;
        private Integer humidity;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Weather {
        private String main;
        private String description;
        private String icon;
    }

    @Data
    public static class Rain {
        @JsonProperty("3h")
        private Double threeHour;
    }

    @Data
    public static class City {
        private String name;
        private Coord coord;
    }

    @Data
    public static class Coord {
        private Double lat;
        private Double lon;
    }
}