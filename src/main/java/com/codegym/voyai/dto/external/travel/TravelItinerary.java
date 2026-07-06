package com.codegym.voyai.dto.external.travel;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TravelItinerary {

    private String destination;
    private Integer totalDays;
    private List<DayItinerary> itinerary;

    // Enrich sau khi parse
    private DestinationInfo destinationInfo;
    private WeatherSummary weatherSummary;

    private String priceContext;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DayItinerary {
        private Integer day;
        private String date;
        private List<ActivityItem> activities;
        private DailyWeather weather;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ActivityItem {
        private String time;
        private String activity;
        private Double lat;
        private Double lng;
        private Long estimatedCost;
        private String reason;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DestinationInfo {
        private String placeId;
        private String fullName;
        private String address;
        private Double lat;
        private Double lng;
        private List<String> photos;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeatherSummary {
        private Double avgTemp;
        private Double minTemp;
        private Double maxTemp;
        private String condition;
        private Integer humidity;
        private String recommendation;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyWeather {
        private Double temp;
        private String condition;
        private String icon;
        private Integer humidity;
        private Double rainChance;
    }
}