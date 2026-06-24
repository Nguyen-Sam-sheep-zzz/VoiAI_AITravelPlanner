package com.codegym.voyai.model.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TripRequest {

    @NotBlank(message = "Điểm đến không được để trống")
    private String destination;

    @NotNull
    @Min(value = 1, message = "Tối thiểu 1 ngày")
    @Max(value = 30, message = "Tối đa 30 ngày")
    private Integer numDays;

    @NotNull
    private BigDecimal budgetTotal;

    private String currency = "VND";
    private String notes;
    private LocalDate startDate;

    // Tọa độ từ Nominatim (frontend gọi suggest rồi truyền vào)
    @NotNull
    private Double lat;

    @NotNull
    private Double lng;

    private String placeId;

    // Các trường tùy chọn cho Điểm xuất phát
    private String originName;
    private Double originLat;
    private Double originLng;
}